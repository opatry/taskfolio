BEGIN {
  IGNORECASE = 1
  replaced = 0
}

{
  if (index($0, "https://img.shields.io/badge/Coverage-") && !replaced) {
    printf("![Coverage %s%%](https://img.shields.io/badge/Coverage-%s%%25-%s)\n", coverage_percent, coverage_percent, coverage_color)
    replaced = 1
    next
  }
  print
}