import TasksAppShared
import GoogleSignIn
import UIKit

@MainActor
class IOSGoogleAuthenticator: OauthGoogleAuthenticator {

    func authorize(scopes: [OauthGoogleAuthenticatorScope], force: Bool, requestUserAuthorization: @escaping (Any) -> Void) async throws -> String {
        let stringScopes = scopes.compactMap { $0.value }

        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first,
              let rootViewController = window.rootViewController else {
            throw GoogleSignInError.noRootViewController
        }

        guard let clientId = Bundle.main.object(forInfoDictionaryKey: "GIDClientID") as? String else {
            throw GoogleSignInError.missingConfiguration
        }

        if GIDSignIn.sharedInstance.configuration == nil {
            let config: GIDConfiguration
            if !stringScopes.isEmpty {
                config = GIDConfiguration(clientID: clientId, serverClientID: clientId)
            } else {
                config = GIDConfiguration(clientID: clientId)
            }
            GIDSignIn.sharedInstance.configuration = config
        }

        return try await withCheckedThrowingContinuation { continuation in
            // FIXME in Jvm impl, the force means &prompt=consent&access_type=offline
            //  is it needed here? at least no need to sign-out
            //  or on Android .requestOfflineAccess(config.clientId, force)
            if force {
                GIDSignIn.sharedInstance.signOut()
            }

            if !force, let currentUser = GIDSignIn.sharedInstance.currentUser {
                if !stringScopes.isEmpty {
                    currentUser.addScopes(stringScopes, presenting: rootViewController) { result, error in
                        if let error = error {
                            continuation.resume(throwing: error)
                        } else if let user = result {
                            continuation.resume(returning: user.user.userID ?? "")
                        } else {
                            continuation.resume(throwing: GoogleSignInError.unknownError)
                        }
                    }
                } else {
                    continuation.resume(returning: currentUser.userID ?? "")
                }
                return
            }

            GIDSignIn.sharedInstance.signIn(withPresenting: rootViewController) { result, error in
                if let error = error {
                    continuation.resume(throwing: error)
                } else if let user = result?.user {
                    if !stringScopes.isEmpty {
                        user.addScopes(stringScopes, presenting: rootViewController) { scopeResult, scopeError in
                            if let scopeError = scopeError {
                                continuation.resume(throwing: scopeError)
                            } else {
                                continuation.resume(returning: user.userID ?? "")
                            }
                        }
                    } else {
                        continuation.resume(returning: user.userID ?? "")
                    }
                } else {
                    continuation.resume(throwing: GoogleSignInError.unknownError)
                }
            }
        }
    }

    func getToken(grant: any OauthGoogleAuthenticatorGrant) async throws -> OauthGoogleAuthenticatorOAuthToken {
        guard let currentUser = GIDSignIn.sharedInstance.currentUser else {
            throw GoogleSignInError.userNotSignedIn
        }

        return try await withCheckedThrowingContinuation { continuation in
            currentUser.refreshTokensIfNeeded { user, error in
                if let error = error {
                    continuation.resume(throwing: error)
                    return
                }

                guard let user = user else {
                    continuation.resume(throwing: GoogleSignInError.tokenNotAvailable)
                    return
                }
                let accessToken = user.accessToken.tokenString

                let expirationDate = user.accessToken.expirationDate
                let currentDate = Date()
                let expiresIn = Int64(expirationDate?.timeIntervalSince(currentDate) ?? 0)

                let token = OauthGoogleAuthenticatorOAuthToken(
                    accessToken: accessToken,
                    expiresIn: expiresIn,
                    idToken: user.idToken?.tokenString,
                    refreshToken: user.refreshToken.tokenString,
                    scope: user.grantedScopes?.joined(separator: " ") ?? "",
                    tokenType: OauthGoogleAuthenticatorOAuthToken.TokenType.bearer
                )

                continuation.resume(returning: token)
            }
        }
    }
}

// MARK: - Error Types
enum GoogleSignInError: Error, LocalizedError {
    case noRootViewController
    case missingConfiguration
    case configurationFailed
    case userNotSignedIn
    case tokenNotAvailable
    case unknownError

    var errorDescription: String? {
        switch self {
        case .noRootViewController:
            return "Unable to find root view controller"
        case .missingConfiguration:
            return "No GIDClientID found in Info.plist"
        case .configurationFailed:
            return "Failed to configure Google Sign-In"
        case .userNotSignedIn:
            return "User is not signed in"
        case .tokenNotAvailable:
            return "Access token not available"
        case .unknownError:
            return "An unknown error occurred"
        }
    }
}
