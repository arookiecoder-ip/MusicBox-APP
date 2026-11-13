# Multi-user CI workflow

GitHub only executes YAML placed in `.github/workflows/`. This copy lives here
because the automation token used for the branch cannot create or edit workflow
files (`workflow` scope is missing from the personal access token).

A maintainer with workflow permission should move it into place:

```bash
mkdir -p .github/workflows
mv ci/android-multiuser.yml .github/workflows/android-multiuser.yml
git add .github/workflows/android-multiuser.yml
git commit -m "Enable the multi-user build workflow"
git push origin feat/multi-user
```

The workflow uses its own signing cache key, keystore path, alias and password.
It does not read `musicbox-debug-signing-v1` or
`ANDROID_DEBUG_KEYSTORE_BASE64`. Move the generated keystore into a protected
repository secret before production.
