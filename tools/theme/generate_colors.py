# Regenerates core/designsystem/theme/Color.kt palettes (Fidelity: keeps the brand orange vivid).
# Usage: pip install materialyoucolor && python3 generate_colors.py

from materialyoucolor.hct import Hct
from materialyoucolor.palettes.tonal_palette import TonalPalette
from materialyoucolor.dynamiccolor.dynamic_scheme import DynamicScheme
from materialyoucolor.dynamiccolor.variant import Variant
from materialyoucolor.dynamiccolor.material_dynamic_colors import MaterialDynamicColors
orange = Hct.from_int(0xFFFF6B2C); navy = Hct.from_int(0xFF1E1B3A)
mdc = MaterialDynamicColors(spec='2021')
roles = ["primary","onPrimary","primaryContainer","onPrimaryContainer","secondary","onSecondary","secondaryContainer","onSecondaryContainer","tertiary","onTertiary","tertiaryContainer","onTertiaryContainer","error","onError","errorContainer","onErrorContainer","background","onBackground","surface","onSurface","surfaceVariant","onSurfaceVariant","outline","outlineVariant","scrim","inverseSurface","inverseOnSurface","inversePrimary","surfaceDim","surfaceBright","surfaceContainerLowest","surfaceContainerLow","surfaceContainer","surfaceContainerHigh","surfaceContainerHighest"]
def pal(h,c): return TonalPalette.from_hue_and_chroma(h,c)
for dark in (False, True):
    s = DynamicScheme(source_color_hct=orange, variant=Variant.FIDELITY, contrast_level=0.0, is_dark=dark,
        primary_palette=pal(orange.hue, orange.chroma), secondary_palette=pal(navy.hue, 36),
        tertiary_palette=pal(orange.hue+40, 50), neutral_palette=pal(navy.hue, 4), neutral_variant_palette=pal(navy.hue, 8))
    for r in roles:
        sn=''.join('_'+c.lower() if c.isupper() else c for c in r)
        dc=getattr(mdc, sn, None) or getattr(mdc, r)
        print(("Dark" if dark else "Light"), r, f"0x{dc.get_argb(s)&0xFFFFFFFF:08X}")
