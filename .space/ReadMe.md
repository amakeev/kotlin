## Codeowners

This directory contains a `CODEOWNERS` file based on [Space CODEOWNERS syntax](https://www.jetbrains.com/help/space/code-owners.html#codeowners-file-syntax) with additional requirements:
1. Team names have to be in quotations
2. Individual owners have to be added via email address which is linked to a GitHub account (it can be a secondary email for the account)

This file supports a `virtual team` setup, where 3rd party contributors outside JetBrains organization can be added
to `virtual-team-mapping.json` for custom team management.

Upon making changes to `CODEOWNERS` or `virtual-team-mapping.json` files, run `generate-github-codeowners` script to regenerate respective GitHub `CODEOWNERS` file.
Correctness of the generated file can be checked by opening it on GitHub, where a banner with information `This CODEOWNERS file is valid.` should be displayed.