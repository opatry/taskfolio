#!/usr/bin/env bash

if [ -t 1 ] && [ "$(tput colors)" ]; then
  RESET="$(tput sgr0)"
  UNDERLINE="$(tput smul)"
  BOLD="$(tput bold)"
  RED="$(tput setaf 1)"
  RED_BOLD="${RED}${BOLD}"
  GREEN="$(tput setaf 2)"
  GREEN_BOLD="${GREEN}${BOLD}"
  YELLOW="$(tput setaf 3)"
  YELLOW_BOLD="${YELLOW}${BOLD}"
  BLUE="$(tput setaf 4)"
  BLUE_BOLD="${BLUE}${BOLD}"
  MAGENTA="$(tput setaf 5)"
  MAGENTA_BOLD="${MAGENTA}${BOLD}"
else
  RESET=""
  UNDERLINE=""
  BOLD=""
  RED=""
  RED_BOLD=""
  GREEN=""
  GREEN_BOLD=""
  YELLOW=""
  YELLOW_BOLD=""
  BLUE=""
  BLUE_BOLD=""
  MAGENTA=""
  MAGENTA_BOLD=""
fi

export RESET
export UNDERLINE
export BOLD
export RED
export RED_BOLD
export GREEN
export GREEN_BOLD
export YELLOW
export YELLOW_BOLD
export BLUE
export BLUE_BOLD
export MAGENTA
export MAGENTA_BOLD

function step_done()
{
  echo -e " ✅  Done\n"
}

function step_error()
{
  echo -e " ❌  ${RED_BOLD}Error${RESET} ($1)\n"
  exit 1
}

function step()
{
  echo -e " 🏃 $1"
}

function info()
{
  echo -e " ℹ️  $1"
}

function warn()
{
  echo -e "⚠️ $1"
}

function ask_yn_choice()
{
  read -r -p "${1} (y/${BOLD}N${RESET}) " choice
  case "${choice}" in 
    y|Y)
      echo true
    ;;
    *)
      echo false
    ;;
  esac
}