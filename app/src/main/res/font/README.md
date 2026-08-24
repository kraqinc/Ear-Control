# Klavika Bold

This project is wired for a Klavika Bold visual style, but the Klavika font file is proprietary/licensed and is intentionally not bundled here.

To use your licensed copy, place the font file at:
`app/src/main/res/font/klavika_bold.ttf`

Then replace the `FontFamily.SansSerif` assignment in `MainActivity.kt` with:
`FontFamily(Font(R.font.klavika_bold))`
