# Contributing

## Thank You

Thank you for your interest in contributing to Herald! This guide will help you get started.

## Links

- [Website](https://dansplugins.com)
- [Discord](https://discord.gg/xXtuAQ2)

## Requirements

- A GitHub account
- Git installed on your local machine
- A Java IDE or text editor
- A basic understanding of Java

## Getting Started

1. [Sign up for GitHub](https://github.com/signup) if you don't have an account.
2. Fork the repository by clicking **Fork** at the top right of the repo page.
3. Clone your fork: `git clone https://github.com/<your-username>/Herald.git`
4. Open the project in your IDE.
5. Build the plugin: `./gradlew build`
   If you encounter errors, please open an issue.

## Identifying What to Work On

### Issues

Work items are tracked as [GitHub issues](https://github.com/Dans-Plugins/Herald/issues).

### Milestones

Issues are grouped into [milestones](https://github.com/Dans-Plugins/Herald/milestones) representing upcoming releases.

## Making Changes

1. Make sure an issue exists for the work. If not, create one using one of the [issue templates](https://github.com/Dans-Plugins/Herald/issues/new/choose).
2. Switch to `main`: `git checkout main`
3. Create a branch: `git checkout -b feature/<short-description>` (see [Branch names](#branch-names) for the other prefixes).
4. Make your changes.
5. Test your changes.
6. Commit: `git commit -m "Add the thing"` (see [Commit messages](#commit-messages)).
7. Push: `git push origin feature/<short-description>`
8. Open a pull request against `main` with `Closes #<number>` in the description so the issue closes when the PR merges.
9. Address review feedback.

## Commit and Pull Request Conventions

### Branch names

Branches are prefixed by the kind of change they carry:

| Prefix | Used for |
|--------|----------|
| `feature/` | New behaviour or configuration options |
| `fix/` | Bug fixes |
| `docs/` | Documentation-only changes (`README.md`, `CONFIG.md`, `USER_GUIDE.md`, and the like) |
| `chore/` | Release bumps, dependency updates, repository metadata, and other housekeeping |
| `ci/` | Changes under `.github/workflows/` |

For example: `feature/smtp-implicit-tls`, `fix/dev-release-retry`, `docs/config-guide-placeholders`, `chore/release-2.0.0`.

### Commit messages

- Write the subject line in the imperative mood, as an instruction: `Add smtp.implicit-tls for SMTPS on port 465`, not `Added ...` or `Adds ...`.
- No trailing period on the subject line.
- Keep the subject under about 72 characters; put any further explanation in the body after a blank line.
- Documentation, release, and CI housekeeping commits may carry a `docs:`, `chore:`, or `ci:` prefix, matching the branch prefix.

Commits authored with an AI agent carry a `Co-Authored-By` trailer on its own line, separated from the body by a blank line. Use a heredoc so the trailer survives quoting:

```bash
git commit -m "$(cat <<'EOF'
Add smtp.implicit-tls for SMTPS on port 465

Co-Authored-By: <agent name> <noreply@anthropic.com>
EOF
)"
```

### Pull requests

- Open pull requests against `main`.
- Reference every issue the pull request resolves with `Closes #<number>` in the description, one per line, so GitHub closes them automatically on merge.
- Include a short summary of what changed and why, and a test plan describing how the change was verified.
- Document any new config option in [CONFIG.md](CONFIG.md) and add an entry under `[Unreleased]` in [CHANGELOG.md](CHANGELOG.md) in the same pull request.

### Merge strategy

Pull requests are **squash-merged** into `main`, so each pull request lands as one commit titled after the pull request and suffixed with its number (for example `chore: 2.0.1-SNAPSHOT (#57)`). Keep the pull request title in the same imperative, no-trailing-period style as a commit subject, since it becomes the commit subject on `main`. Delete the branch once the pull request has merged.

Every pull request since #47 has landed this way. Older history contains a few merge commits (for example #38, #43 and #46), so anything that walks `main` expecting exactly one commit per pull request should not assume it of commits before that point.

## Testing

Run the unit tests with:

Linux: `./gradlew clean test`
Windows: `.\gradlew.bat clean test`

For manual testing, start the Docker-based Spigot test server. The image copies the plugin JAR from `build/libs`, so build it first, and `compose.yml` reads its settings from `.env`:

    ./gradlew build
    cp sample.env .env
    ./up.sh

`./down.sh` stops it again. See [Development](README.md#development) in the README for details.

## Questions

Ask in the [Discord server](https://discord.gg/xXtuAQ2).
