# Repositorio y compilaciones

El código está publicado en el repositorio público `josxtm/DARK1-MIRROR-ANDROID`. El PR #1 propone la rama `feat/dark1-live-screen-android` sobre `main` y está abierto como borrador. [Repositorio](https://github.com/josxtm/DARK1-MIRROR-ANDROID) · [PR #1](https://github.com/josxtm/DARK1-MIRROR-ANDROID/pull/1)

El workflow `.github/workflows/apk.yml` compila la APK de depuración, ejecuta las pruebas JVM y sube la APK y su SHA-256 como artefacto de cada ejecución correcta. Abre la ejecución desde el PR para descargar el archivo. [GitHub Actions](https://github.com/josxtm/DARK1-MIRROR-ANDROID/actions)

El Git Bundle incluido en el paquete de fuentes permite recuperar ambas ramas sin acceder a GitHub.

```sh
git clone DARK1-MIRROR-ANDROID.bundle DARK1-MIRROR-ANDROID
cd DARK1-MIRROR-ANDROID
git branch --all
```
