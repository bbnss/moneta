# Moneta 0.2.3 publication review

Reviewed on 9 October 2026 before integrating the user-tested 0.2.3 into `main`.

## Scope and results

- Scanned all 194 files included in the updated repository, 320 reachable historical Git blobs, commit/tag metadata and the unpacked signed APK/AAB. Checked actual local signing-password values in memory, private-key markers, common API/token patterns, service-account credentials and sensitive file names. No credential findings; no secret values were printed or copied into this report.
- `moneta-upload-key.jks`, `keystore.properties` and `local.properties` are ignored and untracked. The ignore rules now also cover local environment files, common service-account/configuration files and debug keystores.
- No personal filesystem paths remain in the current tracked files or release archives. Reviewed tracked image metadata: no GPS, owner or contact fields. The remaining email-shaped test URL is deliberately invalid example data; the two email-shaped byte sequences in the DEX are binary false positives.
- Replaced the previously public contact email in the current privacy policy with the GitHub issue link. Historical privacy-policy revisions still contain that email; two historical specification blobs still contain a local filesystem path. These were already public. This integration preserves Git history and does not erase previously published revisions or copies.
- Build/signing instructions reference local property names rather than embedding their values. No application runtime source, resource, dependency or version changes are introduced by this review.

Pattern scanning is a targeted check, not a guarantee that arbitrary encoded secrets can never exist.

## Artifact identity

The tested APK and the local AAB were built together from commit `88dce21df0dd504823f2100ad948e8f1c84c75ca`, tag `v0.2.3`. Subsequent privacy/ignore/documentation changes leave that application source unchanged. Keep these exact artifacts for distribution; no rebuild or new version is needed for the repository integration.

| Artifact | SHA-256 |
|---|---|
| `Moneta-0.2.3-9.apk` | `30faa016541663dffc009a4f9d3f4f3999409199b345a205242f5431d7daefc6` |
| `Moneta-0.2.3-9.aab` (local only) | `0cfaf7846db7e80f05f6953e6e2a9a6b98dfd1501bcf469f37d1069c3ce593a3` |

Both use package `it.bbnss.moneta`, version name `0.2.3`, version code `9` and the existing upload certificate SHA-256 `e674d49ab66f68eba081e7a4ffbbd4f4c6a78d7d2e8bc3add7f4517f904c2c02`. APK signature verification, AAB JAR signature verification, bundle validation and manifest checks passed again. The Play-distributed app-signing certificate has not been compared directly with the GitHub APK certificate.

GitHub continues to distribute the existing stable APK release. The AAB stays in ignored local `dist/v0.2.3/Moneta-0.2.3-9.aab`, ready for the subsequent Play Console upload; no Play upload is part of this integration. Functional verification is recorded in [0.2.3 verification](VERIFICATION_0.2.3.md).
