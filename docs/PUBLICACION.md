# Publicar ramas y generar el Pull Request

El destino solicitado es `josxtm/DARK1-MIRROR-ANDROID`. La credencial de la integración pudo consultar la cuenta `josxtm`. GitHub devolvió HTTP 403, «Resource not accessible by integration», al intentar crear ese repositorio. El archivo Git Bundle contiene ambas ramas completas.

Para publicar desde una sesión de GitHub autorizada para crear y escribir el repositorio:

```sh
git clone DARK1-MIRROR-ANDROID.bundle DARK1-MIRROR-ANDROID
cd DARK1-MIRROR-ANDROID
git branch main origin/main
git remote rename origin bundle
gh repo create josxtm/DARK1-MIRROR-ANDROID --private
git remote add origin https://github.com/josxtm/DARK1-MIRROR-ANDROID.git
git push -u origin main
git push -u origin feat/dark1-live-screen-android
gh pr create --repo josxtm/DARK1-MIRROR-ANDROID --draft \
  --base main --head feat/dark1-live-screen-android \
  --title "DARK-1 Mirror para tablet Android" --body-file docs/PR.md
```

Si el repositorio de destino ya existe, consulta sus ramas antes de publicarlas. El workflow `.github/workflows/apk.yml` construye la APK y sube el archivo y su SHA-256 a GitHub Actions.
