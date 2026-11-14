const fs=require('fs'),j='src/main/java/com/agrievolution/',t='src/test/java/com/agrievolution/';
const {names,field,lab}=require('./resource9-spec.json'),up=n=>n.toUpperCase(),title=n=>n.split('_').map(w=>w[0].toUpperCase()+w.slice(1)).join(' ');
const blocks=["HAY_BLOCK","JUNGLE_LOG","HAY_BLOCK","MOSS_BLOCK","HAY_BLOCK","MOSS_BLOCK","TERRACOTTA","BROWN_MUSHROOM_BLOCK","SOUL_SAND","END_STONE","CRIMSON_NYLIUM","WARPED_NYLIUM"];
const speeds=[2,2,2,2,3,3,3,3,4,4,4,4];
const edit=(p,f)=>fs.writeFileSync(p,f(fs.readFileSync(p,'utf8')));
if(fs.readFileSync(j+'farming/ResourceCropRules.java','utf8').includes('case SUGAR ->'))throw Error('Already applied');
edit(j+'genetics/CropSpecies.java',s=>s.replace('<= PEARLESCENT_FROGLIGHT.ordinal()','<= WARPED_FUNGUS.ordinal()'));
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
edit(t+'genetics/ResourceProgressionSelfTest.java',s=>s.replace('public static void main(String[] args) {','public static void main(String[] args) {\n'+[['field',field],['lab',lab]].flatMap(([fn,rs])=>rs.map(([a,b,c,p])=>'        '+fn+'(CropSpecies.'+a+',CropSpecies.'+b+',CropSpecies.'+c+','+p+');')).join('\n')).replace('count()!=109','count()!=121').replace('CropSpecies.OCHRE_FROGLIGHT.ordinal()!=130','CropSpecies.OCHRE_FROGLIGHT.ordinal()!=130||CropSpecies.PEARLESCENT_FROGLIGHT.ordinal()!=142'));
edit(t+'genetics/SpeciesSmokeTest.java',s=>s.replace('length != 143','length != 155'));
edit(t+'client/journal/JournalNavigationSelfTest.java',s=>s.replaceAll('CropSpecies.PEARLESCENT_FROGLIGHT','CropSpecies.WARPED_FUNGUS').replace('== 36','== 39').replace('thirty-six species','thirty-nine species').replaceAll('==140','==152').replaceAll('<=140','<=152').replace('all 138 crosses','all 150 crosses').replace('==92','==100').replace('==46','==50').replace('all 143 breeding','all 155 breeding'));
let test=fs.readFileSync(t+'content/ResourceExpansionEightGameTests.java','utf8');
edit(t+'content/ResourceExpansionEightGameTests.java',s=>s.replaceAll('>=CropSpecies.SPRUCE_LOG.ordinal()','>=CropSpecies.SPRUCE_LOG.ordinal()&&species.ordinal()<=CropSpecies.PEARLESCENT_FROGLIGHT.ordinal()'));
test=test.replaceAll('ResourceExpansionEight','ResourceExpansionNine')
 .replace(/var species=new CropSpecies\[\]\{.*?\};/,'var species=new CropSpecies[]{'+names.map(n=>'CropSpecies.'+up(n)).join(',')+'};')
 .replace(/var blocks=new Block\[\]\{.*?\};/,'var blocks=new Block[]{'+blocks.map(n=>'Blocks.'+n).join(',')+'};')
 .replace(/float\[\] speeds=\{.*?\}/,'float[] speeds={'+speeds.join(',')+'}')
 .replaceAll('>=CropSpecies.SPRUCE_LOG.ordinal()','>=CropSpecies.SUGAR.ordinal()')
 .replace('species==CropSpecies.CRIMSON_STEM||species==CropSpecies.WARPED_STEM||species==CropSpecies.VERDANT_FROGLIGHT||species==CropSpecies.PEARLESCENT_FROGLIGHT',names.slice(8).map(n=>'species==CropSpecies.'+up(n)).join('||'));
fs.writeFileSync(t+'content/ResourceExpansionNineGameTests.java',test);
edit('src/guiPreview/java/com/agrievolution/preview/GrandmasJournalPreview.java',s=>s.replace('CropSpecies.SPRUCE_LOG.ordinal()','CropSpecies.SUGAR.ordinal()'));

