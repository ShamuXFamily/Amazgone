# Regenerates core/designsystem/theme/Color.kt palettes (Fidelity: keeps the brand orange vivid).
# Usage: pip install materialyoucolor && python3 generate_colors.py

from materialyoucolor.hct import Hct
from materialyoucolor.palettes.tonal_palette import TonalPalette
from materialyoucolor.dynamiccolor.dynamic_scheme import DynamicScheme
from materialyoucolor.dynamiccolor.variant import Variant
from materialyoucolor.dynamiccolor.material_dynamic_colors import MaterialDynamicColors
orange = Hct.from_int(0xFFFF6B2C); navy = Hct.from_int(0xFF1E1B3A)
mdc = MaterialDynamicColors(spec='2021')
# Brand overrides on top of the generated scheme:
# - primary is the exact vivid brand orange (buttons, tabs, focus) with white content
# - one surface rule: grey page (surface/background), white cards (surfaceContainerLowest/Low)
OVERRIDES = {
    "Light": {
        "primary": 0xFFFF6B2C, "onPrimary": 0xFFFFFFFF, "primaryContainer": 0xFFFFE3D6, "onPrimaryContainer": 0xFF5C1C00,
        "inversePrimary": 0xFFFFB59A,
        "secondaryContainer": 0xFFEDEBF3, "onSecondaryContainer": 0xFF1E1B3A,
        # tertiary = coin gold (coins, levels, trophies)
        "tertiary": 0xFFB77A00, "onTertiary": 0xFFFFFFFF, "tertiaryContainer": 0xFFFFE7B0, "onTertiaryContainer": 0xFF4A3000,
        "background": 0xFFF5F5F8, "surface": 0xFFF5F5F8, "surfaceDim": 0xFFE4E3E8, "surfaceBright": 0xFFF5F5F8,
        "surfaceContainerLowest": 0xFFFFFFFF, "surfaceContainerLow": 0xFFFFFFFF, "surfaceContainer": 0xFFEFEEF3,
        "surfaceContainerHigh": 0xFFEAE9EF, "surfaceContainerHighest": 0xFFE3E2E8,
    },
    "Dark": {
        "primary": 0xFFFF7A3F, "onPrimary": 0xFF2A0C00, "secondaryContainer": 0xFF2E2C38, "onSecondaryContainer": 0xFFE6E3F2,
        "tertiary": 0xFFFFC04D, "onTertiary": 0xFF3D2800, "tertiaryContainer": 0xFF5A3F00, "onTertiaryContainer": 0xFFFFE7B0, "primaryContainer": 0xFF6B2A0B, "onPrimaryContainer": 0xFFFFDBCC,
        "background": 0xFF121116, "surface": 0xFF121116, "surfaceContainerLowest": 0xFF1D1C22, "surfaceContainerLow": 0xFF1D1C22,
        "surfaceContainer": 0xFF232228, "surfaceContainerHigh": 0xFF2B2A30, "surfaceContainerHighest": 0xFF35343A,
    },
}
roles = ["primary","onPrimary","primaryContainer","onPrimaryContainer","secondary","onSecondary","secondaryContainer","onSecondaryContainer","tertiary","onTertiary","tertiaryContainer","onTertiaryContainer","error","onError","errorContainer","onErrorContainer","background","onBackground","surface","onSurface","surfaceVariant","onSurfaceVariant","outline","outlineVariant","scrim","inverseSurface","inverseOnSurface","inversePrimary","surfaceDim","surfaceBright","surfaceContainerLowest","surfaceContainerLow","surfaceContainer","surfaceContainerHigh","surfaceContainerHighest"]
def pal(h,c): return TonalPalette.from_hue_and_chroma(h,c)
for dark in (False, True):
    s = DynamicScheme(source_color_hct=orange, variant=Variant.FIDELITY, contrast_level=0.0, is_dark=dark,
        primary_palette=pal(orange.hue, orange.chroma), secondary_palette=pal(navy.hue, 14),
        tertiary_palette=pal(orange.hue+40, 50), neutral_palette=pal(navy.hue, 4), neutral_variant_palette=pal(navy.hue, 8))
    mode = "Dark" if dark else "Light"
    for r in roles:
        sn=''.join('_'+c.lower() if c.isupper() else c for c in r)
        dc=getattr(mdc, sn, None) or getattr(mdc, r)
        value = OVERRIDES[mode].get(r, dc.get_argb(s) & 0xFFFFFFFF)
        print(mode, r, f"0x{value:08X}")
