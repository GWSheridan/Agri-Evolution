const fs=require('fs'),j='src/main/java/com/agrievolution/',t='src/test/java/com/agrievolution/';
const {names,field,lab}=require('./resource8-spec.json'),up=n=>n.toUpperCase(),title=n=>n.split('_').map(w=>w[0].toUpperCase()+w.slice(1)).join(' ');
const blocks=["SPRUCE_LOG","BIRCH_LOG","ACACIA_LOG","DARK_OAK_LOG","MANGROVE_LOG","CHERRY_LOG","PRISMARINE","MOSS_BLOCK","CRIMSON_NYLIUM","WARPED_NYLIUM","VERDANT_FROGLIGHT","PEARLESCENT_FROGLIGHT"];
const speeds=[2,2,2,2,3,3,3,3,4,4,5,5];
const edit=(p,f)=>fs.writeFileSync(p,f(fs.readFileSync(p,'utf8')));
if(fs.readFileSync(j+'farming/ResourceCropRules.java','utf8').includes('case SPRUCE_LOG ->'))throw Error('Already applied');
edit(j+'genetics/CropSpecies.java',s=>s.replace('<= OCHRE_FROGLIGHT.ordinal()','<= PEARLESCENT_FROGLIGHT.ordinal()'));
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
edit(t+'genetics/ResourceProgressionSelfTest.java',s=>s.replace('public static void main(String[] args) {','public static void main(String[] args) {\n'+[['field',field],['lab',lab]].flatMap(([fn,rs])=>rs.map(([a,b,c,p])=>'        '+fn+'(CropSpecies.'+a+',CropSpecies.'+b+',CropSpecies.'+c+','+p+');')).join('\n')).replace('count()!=97','count()!=109').replace('CropSpecies.ENDER_EYE.ordinal()!=118','CropSpecies.ENDER_EYE.ordinal()!=118||CropSpecies.OCHRE_FROGLIGHT.ordinal()!=130'));
edit(t+'genetics/SpeciesSmokeTest.java',s=>s.replace('length != 131','length != 143'));
edit(t+'client/journal/JournalNavigationSelfTest.java',s=>s.replaceAll('CropSpecies.OCHRE_FROGLIGHT','CropSpecies.PEARLESCENT_FROGLIGHT').replace('== 33','== 36').replace('thirty-three species','thirty-six species').replaceAll('==128','==140').replaceAll('<=128','<=140').replace('all 126 crosses','all 138 crosses').replace('==84','==92').replace('==42','==46').replace('all 131 breeding','all 143 breeding'));
let test=fs.readFileSync(t+'content/ResourceExpansionSevenGameTests.java','utf8');
edit(t+'content/ResourceExpansionSevenGameTests.java',s=>s.replaceAll('>=CropSpecies.CHARCOAL.ordinal()','>=CropSpecies.CHARCOAL.ordinal()&&species.ordinal()<=CropSpecies.OCHRE_FROGLIGHT.ordinal()'));
test=test.replaceAll('ResourceExpansionSeven','ResourceExpansionEight')
 .replace(/var species=new CropSpecies\[\]\{.*?\};/,'var species=new CropSpecies[]{'+names.map(n=>'CropSpecies.'+up(n)).join(',')+'};')
 .replace(/var blocks=new Block\[\]\{.*?\};/,'var blocks=new Block[]{'+blocks.map(n=>'Blocks.'+n).join(',')+'};')
 .replace(/float\[\] speeds=\{.*?\}/,'float[] speeds={'+speeds.join(',')+'}')
 .replaceAll('>=CropSpecies.CHARCOAL.ordinal()','>=CropSpecies.SPRUCE_LOG.ordinal()')
 .replace('species==CropSpecies.DARK_PRISMARINE||species==CropSpecies.GILDED_BLACKSTONE||species==CropSpecies.SCULK_CATALYST||species==CropSpecies.OCHRE_FROGLIGHT',names.slice(8).map(n=>'species==CropSpecies.'+up(n)).join('||'));
fs.writeFileSync(t+'content/ResourceExpansionEightGameTests.java',test);
edit('src/guiPreview/java/com/agrievolution/preview/GrandmasJournalPreview.java',s=>s.replace('CropSpecies.CHARCOAL.ordinal()','CropSpecies.SPRUCE_LOG.ordinal()'));

