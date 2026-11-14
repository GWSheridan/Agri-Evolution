const fs=require('fs'),j='src/main/java/com/agrievolution/',t='src/test/java/com/agrievolution/';
const {names,field,lab}=require('./resource7-spec.json'),up=n=>n.toUpperCase(),title=n=>n.split('_').map(w=>w[0].toUpperCase()+w.slice(1)).join(' ');
const blocks=['COAL_BLOCK','GLASS','BROWN_MUSHROOM_BLOCK','RED_MUSHROOM_BLOCK','SNOW_BLOCK','HONEY_BLOCK','MOSSY_COBBLESTONE','MOSS_BLOCK','DARK_PRISMARINE','GILDED_BLACKSTONE','SCULK_CATALYST','OCHRE_FROGLIGHT'];
const speeds=[2,2,2,2,3,3,3,3,4,4,5,5];
const edit=(p,f)=>fs.writeFileSync(p,f(fs.readFileSync(p,'utf8')));
if(fs.readFileSync(j+'farming/ResourceCropRules.java','utf8').includes('case CHARCOAL ->'))throw Error('Already applied');
edit(j+'genetics/CropSpecies.java',s=>s.replace('<= ENDER_EYE.ordinal()','<= OCHRE_FROGLIGHT.ordinal()'));
edit(j+'farming/ResourceCropRules.java',s=>{
 for(const [key,fn] of [
 ['default -> Items.WHEAT;',n=>'Items.'+up(n)],
 ['default -> "Wheat";',n=>JSON.stringify(title(n))],
 ['default -> "No resource catalyst";',(n,i)=>JSON.stringify(title(blocks[i].toLowerCase())+' below farmland: x'+speeds[i]+'.0')],
 ['default -> false;',(n,i)=>'state.is(Blocks.'+blocks[i]+')'],
 ['default -> 1.0f;',(n,i)=>speeds[i]+'.0f']
 ])s=s.replace(key,names.map((n,i)=>'case '+up(n)+' -> '+fn(n,i)+';').join('\n            ')+'\n            '+key);
 return s;
});
edit(t+'genetics/ResourceProgressionSelfTest.java',s=>s.replace('public static void main(String[] args) {','public static void main(String[] args) {\n'+[['field',field],['lab',lab]].flatMap(([fn,rs])=>rs.map(([a,b,c,p])=>'        '+fn+'(CropSpecies.'+a+',CropSpecies.'+b+',CropSpecies.'+c+','+p+');')).join('\n')).replace('count()!=85','count()!=97').replace('CropSpecies.SCULK.ordinal()!=106','CropSpecies.SCULK.ordinal()!=106||CropSpecies.ENDER_EYE.ordinal()!=118'));
edit(t+'genetics/SpeciesSmokeTest.java',s=>s.replace('length != 119','length != 131'));
edit(t+'client/journal/JournalNavigationSelfTest.java',s=>s.replaceAll('CropSpecies.ENDER_EYE','CropSpecies.OCHRE_FROGLIGHT').replace('== 30','== 33').replace('thirty species','thirty-three species').replaceAll('==116','==128').replaceAll('<=116','<=128').replace('all 114 crosses','all 126 crosses').replace('==76','==84').replace('==38','==42').replace('all 119 breeding','all 131 breeding'));
let test=fs.readFileSync(t+'content/ResourceExpansionSixGameTests.java','utf8');
edit(t+'content/ResourceExpansionSixGameTests.java',s=>s.replaceAll('>=CropSpecies.RED_SAND.ordinal()','>=CropSpecies.RED_SAND.ordinal()&&species.ordinal()<=CropSpecies.ENDER_EYE.ordinal()'));
test=test.replaceAll('ResourceExpansionSix','ResourceExpansionSeven')
 .replace(/var species=new CropSpecies\[\]\{.*?\};/,'var species=new CropSpecies[]{'+names.map(n=>'CropSpecies.'+up(n)).join(',')+'};')
 .replace(/var blocks=new Block\[\]\{.*?\};/,'var blocks=new Block[]{'+blocks.map(n=>'Blocks.'+n).join(',')+'};')
 .replace(/float\[\] speeds=\{.*?\}/,'float[] speeds={'+speeds.join(',')+'}')
 .replaceAll('>=CropSpecies.RED_SAND.ordinal()','>=CropSpecies.CHARCOAL.ordinal()')
 .replace('species==CropSpecies.BLUE_ICE||species==CropSpecies.MAGMA_BLOCK||species==CropSpecies.NETHER_BRICK||species==CropSpecies.ENDER_EYE',names.slice(8).map(n=>'species==CropSpecies.'+up(n)).join('||'));
fs.writeFileSync(t+'content/ResourceExpansionSevenGameTests.java',test);
edit('src/guiPreview/java/com/agrievolution/preview/GrandmasJournalPreview.java',s=>s.replace('CropSpecies.RED_SAND.ordinal()','CropSpecies.CHARCOAL.ordinal()'));

