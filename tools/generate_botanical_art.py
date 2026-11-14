"""Agri Evolution 0.5.0 original procedural pixel art, no external source textures."""
from PIL import Image, ImageDraw
from pathlib import Path
from collections import defaultdict
import json
ROOT = Path(__file__).resolve().parents[1]/'src/main/resources/assets/agri_evolution/textures'
GUI = ROOT/'gui/journal/species'
BLOCK = ROOT/'block'
ITEM = ROOT/'item'
PROFILES={
 'amber_marigold': {'stem':'#447b39','leaf':'#76b350','shade':'#2f5c34','flower':'#f6a12e','edge':'#ad4e1c','inner':'#ffe274','centre':'#964528','petals':6},
 'silver_thistle': {'stem':'#496e67','leaf':'#82a7a0','shade':'#315052','flower':'#b3b6c8','edge':'#696e91','inner':'#d8deed','centre':'#8893b0','petals':7},
 'sunfire_lily': {'stem':'#37743c','leaf':'#5aad55','shade':'#22552d','flower':'#f26426','edge':'#ae3725','inner':'#ffd45b','centre':'#fff2a7','petals':5},
 'moon_orchid': {'stem':'#326b6a','leaf':'#70afa9','shade':'#275255','flower':'#c6a9f0','edge':'#7158ac','inner':'#f5defe','centre':'#67f3e5','petals':5},
}
def color(h):return tuple(bytes.fromhex(h.lstrip('#')))+(255,)
def leaf(d,x,y,side,p):
 # pointed 3x3 pixel leaf with dark lower edge
 if side==-1:
  d.polygon([(x,y),(x-2,y-2),(x-5,y-2),(x-4,y),(x-1,y+1)],fill=color(p['leaf']))
  d.line([(x-4,y),(x-1,y+1)],fill=color(p['shade']))
 else:
  d.polygon([(x,y),(x+2,y-2),(x+5,y-2),(x+4,y),(x+1,y+1)],fill=color(p['leaf']))
  d.line([(x+4,y),(x+1,y+1)],fill=color(p['shade']))
def draw_flower(d,x,y,p,kind):
 # legacy 0.5.0 journal illustration silhouettes
 edge=color(p['edge']);outer=color(p['flower']);bright=color(p['inner']);center=color(p['centre'])
 if kind=='amber_marigold':
  d.rectangle((x-2,y-3,x+2,y+3),fill=edge)
  for dx,dy in [(-3,-1),(-3,1),(3,-1),(3,1),(-1,-3),(1,-3),(-1,3),(1,3)]: d.point((x+dx,y+dy),fill=outer)
  d.rectangle((x-2,y-2,x+2,y+2),fill=outer)
  d.rectangle((x-1,y-1,x+1,y+1),fill=bright)
  d.point((x,y),fill=center)
 elif kind=='silver_thistle':
  for dx,dy in [(-3,0),(3,0),(-2,-2),(2,-2),(-1,-4),(1,-4),(-3,-2),(3,-2)]:d.point((x+dx,y+dy),fill=bright)
  d.polygon([(x,y-4),(x+2,y-2),(x+3,y+1),(x+1,y+3),(x-2,y+2),(x-3,y-1)],fill=edge)
  d.polygon([(x-1,y-3),(x+1,y-3),(x+2,y-1),(x+1,y+1),(x-1,y+1),(x-2,y-1)],fill=outer)
  d.point((x,y),fill=bright)
 elif kind=='sunfire_lily':
  for dx,dy in [(-3,-1),(-2,-3),(0,-4),(2,-3),(3,-1),(-2,2),(2,2)]:d.point((x+dx,y+dy),fill=edge)
  d.polygon([(x,y-3),(x+3,y-1),(x+2,y+2),(x,y+3),(x-2,y+2),(x-3,y-1)],fill=outer)
  d.rectangle((x-1,y-2,x+1,y+1),fill=bright)
  d.point((x,y),fill=center)
 elif kind=='moon_orchid':
  for dx,dy in [(-3,-1),(-2,-3),(0,-4),(2,-3),(3,-1),(-3,1),(3,1),(0,3)]: d.point((x+dx,y+dy),fill=edge)
  d.polygon([(x-3,y-1),(x-2,y-3),(x,y-2),(x+2,y-3),(x+3,y-1),(x+2,y+2),(x,y+3),(x-2,y+2)],fill=outer)
  d.rectangle((x-1,y-2,x+1,y+1),fill=bright)
  d.point((x,y),fill=center)

def legacy_crop(slug,stage):
 p=PROFILES[slug]; im=Image.new('RGBA',(16,16)); d=ImageDraw.Draw(im)
 # stepwise emergence 0..7 with strong visual differentiation
 tip = max(3,14-int(stage*1.5))
 if stage==0:
  d.rectangle((7,13,8,15),fill=color(p['stem']))
  d.point((6,14),fill=color(p['leaf']))
 else:
  d.line([(8,15),(8,tip)],fill=color(p['stem']),width=2)
  d.line([(9,15),(9,max(tip+1,14))],fill=color(p['shade']))
  for j in range(min(stage,4)):
   ly=14-j*2
   if ly>tip+1: leaf(d,8,ly,-1 if j%2==0 else 1,p)
  if stage>=4:
   d.line([(8,10),(5,8)],fill=color(p['stem']),width=1)
   leaf(d,5,9,-1,p)
  if stage>=5:
   d.line([(8,11),(11,7)],fill=color(p['stem']),width=1)
   leaf(d,11,8,1,p)
  if stage>=6:
   draw_flower(d,8,tip+1,p,slug)
  if stage==7:
   # mature bloom's second bud keeps geometry original and fuller than earlier stages
   if slug=='amber_marigold':
    d.line([(8,12),(4,7)],fill=color(p['stem']))
    d.ellipse((2,4,5,7), fill=color(p['flower']))
    d.point((3,5), fill=color(p['inner']))
   if slug=='silver_thistle':
    d.point((4,6),fill=color(p['inner']));d.point((11,5),fill=color(p['flower']))
   if slug=='sunfire_lily':
    d.point((4,5),fill=color(p['inner']));d.point((12,4),fill=color(p['inner']))
   if slug=='moon_orchid':
    d.point((4,4),fill=color(p['inner']));d.point((12,5),fill=color(p['inner']))
 return im

# 0.5.1 slender in-world crop silhouettes. The 0.5.0 method above is retained
# solely for the established journal illustration artwork; it is not rendered
# in the world. Keep the crop models consistent with the existing four plants.
def tiny_flower(d, x, y, p, slug, mature):
    edge=color(p['edge']); petal=color(p['flower'])
    highlight=color(p['inner']); core=color(p['centre'])
    if not mature:
        # Two small, open buds in stage 6; mature petals unfold at stage 7.
        d.point((x,y-2), fill=highlight)
        for dx,dy in [(-1,-1),(0,-1),(1,-1),(1,0),(0,1)]:
            d.point((x+dx,y+dy),fill=petal)
        d.point((x-1,0+y),fill=edge)
        d.point((x,y), fill=core)
        return
    if slug=='amber_marigold':
        # Compact five-wide golden ring with orange petal tips.
        for dx,dy in [(0,-2),(-1,-1),(1,-1),(-2,0),(2,0),(-1,1),(1,1),(0,2)]:
            d.point((x+dx,y+dy),fill=edge)
        for dx,dy in [(-1,-2),(1,-2),(-2,-1),(2,-1),(-2,1),(2,1),(-1,2),(1,2)]:
            d.point((x+dx,y+dy),fill=petal)
        d.rectangle((x-1,y-1,x+1,y+1), fill=highlight)
        d.point((x,y),fill=core)
    elif slug=='silver_thistle':
        # Upright thistle spike; narrower than a conventional rounded bloom.
        for dx,dy in [(0,-3),(-1,-2),(1,-2),(-2,-1),(2,-1),(-2,0),(2,0),(-1,1),(1,1)]:
            d.point((x+dx,y+dy),fill=edge)
        for dx,dy in [(0,-2),(-1,-1),(0,-1),(1,-1),(-1,0),(0,0),(1,0)]:
            d.point((x+dx,y+dy),fill=highlight)
        d.point((x,y+1),fill=petal)
        d.point((x,y+2),fill=color(p['shade']))
    elif slug=='sunfire_lily':
        # Open pointed lily, with a slim shape and yellow centre.
        for dx,dy in [(0,-3),(-2,-2),(2,-2),(-2,1),(2,1),(0,2)]:
            d.point((x+dx,y+dy),fill=edge)
        for dx,dy in [(-1,-2),(1,-2),(-2,-1),(2,-1),(-1,0),(1,0),(-1,1),(1,1)]:
            d.point((x+dx,y+dy),fill=petal)
        for dx,dy in [(0,-1),(-1,0),(0,0),(1,0),(0,1)]:
            d.point((x+dx,y+dy),fill=highlight)
        d.point((x,y),fill=core)
    else:
        # Moon orchid: distinct lobes, pale petals and an aqua centre.
        for dx,dy in [(0,-2),(-2,-1),(2,-1),(-2,1),(2,1),(0,2)]:
            d.point((x+dx,y+dy),fill=edge)
        for dx,dy in [(-1,-2),(1,-2),(-2,0),(2,0),(-1,1),(1,1),(0,1)]:
            d.point((x+dx,y+dy),fill=petal)
        for dx,dy in [(-1,-1),(0,-1),(1,-1),(-1,0),(1,0)]:
            d.point((x+dx,y+dy),fill=highlight)
        d.point((x,y),fill=core)

def crop(slug,stage):
    p=PROFILES[slug]
    im=Image.new('RGBA',(16,16),(0,0,0,0))
    d=ImageDraw.Draw(im)
    stem=color(p['stem']); shade=color(p['shade']); light=color(p['leaf'])
    # Two independent stems instead of one huge centred cluster. Mature flower
    # heads each span ~5 pixels, as in the 0.4.5 Crimson/Violet crops.
    for left, x in enumerate((4,11)):
        if stage==0:
            d.point((x,15),fill=stem)
            d.point((x-1 if left==0 else x+1,15),fill=light)
            continue
        top={1:13,2:11,3:10,4:9,5:8,6:8,7:8}[stage]
        d.line((x,15,x,top),fill=stem)
        d.point((x,15),fill=shade)
        if stage>=2:
            # Opposed little leaves, not long horizontal lines across the block.
            side=-1 if left==0 else 1
            d.point((x+side,12),fill=light)
            d.point((x+2*side,12),fill=light)
            d.point((x+side,13),fill=shade)
        if stage>=3:
            side=1 if left==0 else -1
            d.point((x+side,10),fill=light)
            d.point((x+2*side,10),fill=light)
            d.point((x+side,11),fill=stem)
        if stage>=4:
            side=-1 if left==0 else 1
            d.point((x+side,9),fill=light)
            d.point((x+side,10),fill=stem)
        if stage==5:
            d.point((x,7),fill=color(p['edge']))
            d.point((x,6),fill=color(p['flower']))
        if stage>=6:
            # Offset the two blooms naturally; keep a clear gap between them.
            tiny_flower(d,x,6 if left==0 else 5,p,slug,stage==7)
    return im

def seed_art(slug):
 p=PROFILES[slug]; im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
 # sturdy seed pouch with sprout and coloured botanical seal
 d.polygon([(4,5),(12,5),(14,8),(13,13),(11,15),(4,15),(2,13),(2,9)], fill=(67,38,24,255))
 d.rectangle((4,8,12,13), fill=(132,86,49,255))
 d.line([(3,7),(12,7)],fill=(208,153,89,255),width=1)
 d.rectangle((5,10,10,12),fill=color(p['edge']))
 d.rectangle((6,10,9,11),fill=color(p['flower']))
 d.line([(8,6),(8,2)],fill=color(p['stem']))
 d.polygon([(8,4),(6,2),(4,2),(5,4)],fill=color(p['leaf']))
 d.polygon([(8,3),(11,1),(12,2),(10,4)],fill=color(p['leaf']))
 d.point((5,14),fill=(211,160,92,255))
 return im

def journal_icon(slug):
 p=PROFILES[slug]
 # consistent parchment-tiled 32px illustrations built from original pixel artwork
 im=Image.new('RGBA',(32,32), (0,0,0,0));d=ImageDraw.Draw(im)
 d.rectangle((0,0,31,31),fill=(52,65,54,255))
 d.rectangle((2,2,29,29),fill=(158,143,108,255))
 d.rectangle((4,4,27,27),fill=(232,217,171,255))
 d.line([(6,6),(25,6)],fill=(249,233,197,255))
 plant=legacy_crop(slug,7).resize((32,32),Image.Resampling.NEAREST)
 im.alpha_composite(plant)
 return im

for slug in PROFILES:
 for stage in range(8):
  crop(slug,stage).save(BLOCK/f'{slug}_crop_stage{stage}.png')
 seed_art(slug).save(ITEM/f'{slug}_seed.png')
 icon=journal_icon(slug)
 icon.save(GUI/f'{slug}.png')
 icon.resize((16,16),Image.Resampling.NEAREST).save(GUI/'small'/f'{slug}.png')
 for name in [GUI/f'{slug}.png', GUI/'small'/f'{slug}.png']:
  (name.parent/(name.name+'.mcmeta')).write_text(json.dumps({'texture':{'blur':False,'clamp':False}})+'\n')
print('Generated', len(PROFILES)*11, 'original PNGs for new plants')
