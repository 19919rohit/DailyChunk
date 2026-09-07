# Contributing to DailyChunk

Thank you for your interest in contributing to DailyChunk! ❤️

DailyChunk is an open-source Android download manager built for people who need better control over large downloads and limited or metered internet.

Contributions of all kinds are welcome — bug fixes, improvements, documentation, testing, and new ideas.

## Before You Start

If you are planning a **large feature or significant change**, please open an issue first and discuss the idea before starting work.

This helps make sure the proposed change fits the project's goals and saves everyone time.

For small bug fixes, documentation changes, and other straightforward improvements, you can usually go ahead and open a pull request.

## Getting Started

You'll need:

- Android Studio
- A compatible JDK
- Git
- Basic familiarity with Kotlin and Android development

Clone the repository:

```bash
git clone https://github.com/19919rohit/DailyChunk.git
cd DailyChunk
```

Open the project in Android Studio and allow Gradle to sync.

Before making changes, make sure the project builds successfully.

## Project Structure

The main application code is located at:

```text
app/src/main/java/neunix/dailychunk/
```

The project is organized into several main packages:

- `data/` — Database, repositories, preferences, and queue management
- `download/` — Download engine and background download services
- `notification/` — Notification-related functionality
- `ui/` — Jetpack Compose screens, navigation, and ViewModels
- `util/` — Utility and helper classes
- `work/` — WorkManager workers and scheduling

Try to keep new code in the appropriate package and avoid unrelated changes.

## Creating a Branch

Please create a separate branch for your work instead of committing directly to `main`.

For example:

```bash
git checkout -b fix/download-resume
```

Some examples:

```text
fix/download-resume
fix/notification-crash
feature/download-scheduling
feature/new-settings
docs/update-readme
```

Use a short and descriptive branch name.

## Making Changes

When working on a contribution:

1. Keep the change focused on one purpose.
2. Follow the existing project structure and coding style.
3. Avoid unnecessary dependencies.
4. Avoid unrelated changes.
5. Keep the code simple and maintainable.
6. Test your changes before opening a pull request.

Small, focused changes are easier to review and more likely to be accepted.

## Testing

Before opening a pull request, make sure:

- The project builds successfully.
- The affected functionality works as expected.
- Existing functionality still works.
- Error and failure cases are handled where appropriate.
- The changes have been tested on an Android device or emulator when possible.

Build the project with:

```bash
./gradlew build
```

On Windows:

```bash
gradlew.bat build
```

## Android-Specific Considerations

DailyChunk relies on downloads, background work, scheduling, and notifications.

If your changes affect these areas, consider testing:

- Network loss and recovery
- Download interruption and resumption
- Device restarts
- Background execution
- Battery usage
- Wi-Fi and mobile-data behavior
- Notifications
- File and storage handling
- Different Android versions

Avoid changes that unnecessarily increase battery, network, or storage usage.

## Bug Reports

Before opening a bug report, please search the existing issues to make sure the problem hasn't already been reported.

A useful bug report should include:

- A clear description of the problem
- Steps to reproduce it
- Expected behavior
- Actual behavior
- Android version
- Device model, when relevant
- Relevant logs or screenshots

Please remove personal or sensitive information before sharing logs or screenshots.

## Feature Requests

Have an idea for DailyChunk?

We'd be happy to hear it.

When proposing a feature, explain:

- What problem it solves
- Why it would be useful
- How you think it should work
- Any alternatives or limitations you have considered

For larger features, please open an issue for discussion before starting implementation.

## Pull Requests

When your changes are ready:

1. Push your branch to your fork or repository.
2. Open a pull request against `main`.
3. Clearly explain what you changed and why.
4. Link any related issues.
5. Mention how you tested the changes.
6. Review your own changes before requesting review.

### Pull Request Checklist

Before submitting, please make sure:

- [ ] The project builds successfully.
- [ ] I tested my changes.
- [ ] Existing functionality still works.
- [ ] My changes are focused and relevant.
- [ ] I removed unnecessary code and imports.
- [ ] I have not included personal or sensitive information.
- [ ] I have explained any important implementation decisions.

Pull requests may be reviewed for correctness, maintainability, compatibility, performance, and consistency with the project.

Feedback is a normal part of the review process. Please don't hesitate to make changes based on review comments.

## Commit Messages

Use clear and descriptive commit messages.

Good examples:

```text
Fix download resume after network loss
Add Wi-Fi-only download setting
Improve download progress handling
Update contribution guidelines
```

Avoid vague messages such as:

```text
fix
changes
update
stuff
```

You don't need to follow a complicated commit-message format. Just make sure the message clearly describes what the commit does.

## Code Style

Please follow the existing Kotlin and Android coding style used in the project.

In general:

- Use clear and descriptive names.
- Keep functions and classes focused.
- Prefer simple solutions over unnecessary complexity.
- Reuse existing project utilities where appropriate.
- Remove unused imports and code.
- Add comments when they provide useful context.
- Avoid adding dependencies unless they are necessary.

For Jetpack Compose code, follow the patterns already established in the project.

## Privacy

DailyChunk is designed with user privacy in mind.

Please do not introduce unnecessary:

- Analytics
- Tracking
- Advertising SDKs
- Personal-data collection
- Network requests unrelated to the app's functionality

If a contribution introduces a dependency or service that handles user data, clearly explain why it is needed.

## Dependencies

Before adding a dependency, check whether the existing Android, Kotlin, Jetpack, or project functionality can solve the problem.

If a new dependency is necessary:

- Explain why it is needed.
- Prefer reputable and actively maintained projects.
- Check its license.
- Make sure it is compatible with the project.
- Avoid adding dependencies for functionality that can be implemented reasonably without them.

## Documentation

Documentation improvements are welcome.

If your change affects user-facing behavior, please update the relevant documentation when appropriate.

This may include:

- `README.md`
- `CONTRIBUTING.md`
- Comments
- Other project documentation

## License

By contributing to DailyChunk, you agree that your contributions may be distributed under the same license as the project.

Please see the `LICENSE` file for the applicable license terms.

## Code of Conduct

Please be respectful and constructive when participating in the project.

A dedicated `CODE_OF_CONDUCT.md` may be added to the repository as the community grows.

## Questions

If you're unsure about something, feel free to open an issue and ask.

It's better to discuss an approach early than spend significant time implementing something that doesn't fit the project.

## Thank You

Thank you for taking the time to contribute to DailyChunk!

Whether you're fixing a bug, improving the code, writing documentation, testing the app, or sharing an idea, your contribution is appreciated.