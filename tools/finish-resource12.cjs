const fs=require('fs'),j='src/main/java/com/agrievolution/',t='src/test/java/com/agrievolution/';
const {names,field,lab}=require('./resource12-spec.json'),up=n=>n.toUpperCase(),title=n=>n.split('_').map(w=>w[0].toUpperCase()+w.slice(1)).join(' ');
const blocks=["OAK_LOG","DRIED_KELP_BLOCK","GRAY_TERRACOTTA","LIGHT_GRAY_TERRACOTTA","CALCITE","MOSS_BLOCK","BEEHIVE","SMOOTH_QUARTZ","BONE_BLOCK","SOUL_SAND","TNT","MOSSY_COBBLESTONE"];
const speeds=[2,2,2,2,3,3,3,3,4,5,4,4];
const edit=(p,f)=>fs.writeFileSync(p,f(fs.readFileSync(p,'utf8')));
if(fs.readFileSync(j+'farming/ResourceCropRules.java','utf8').includes('case STICK ->'))throw Error('Already applied');
edit(j+'genetics/CropSpecies.java',s=>s.replace('<= HEAVY_CORE.ordinal()','<= ZOMBIE_HEAD.ordinal()'));
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
edit(t+'genetics/ResourceProgressionSelfTest.java',s=>s.replace('public static void main(String[] args) {','public static void main(String[] args) {\n'+[['field',field],['lab',lab]].flatMap(([fn,rs])=>rs.map(([a,b,c,p])=>'        '+fn+'(CropSpecies.'+a+',CropSpecies.'+b+',CropSpecies.'+c+','+p+');')).join('\n')).replace('count()!=145','count()!=157').replace('CropSpecies.EXPERIENCE_BOTTLE.ordinal()!=166','CropSpecies.EXPERIENCE_BOTTLE.ordinal()!=166||CropSpecies.HEAVY_CORE.ordinal()!=178'));
edit(t+'genetics/SpeciesSmokeTest.java',s=>s.replace('length != 179','length != 191'));
edit(t+'client/journal/JournalNavigationSelfTest.java',s=>s.replaceAll('CropSpecies.HEAVY_CORE','CropSpecies.ZOMBIE_HEAD').replace('== 45','== 48').replace('forty-five species','forty-eight species').replaceAll('==176','==188').replaceAll('<=176','<=188').replace('all 174 crosses','all 186 crosses').replace('==116','==124').replace('==58','==62').replace('all 179 breeding','all 191 breeding'));
let test=fs.readFileSync(t+'content/ResourceExpansionElevenGameTests.java','utf8');
edit(t+'content/ResourceExpansionElevenGameTests.java',s=>s.replaceAll('>=CropSpecies.WHITE_DYE.ordinal()','>=CropSpecies.WHITE_DYE.ordinal()&&species.ordinal()<=CropSpecies.HEAVY_CORE.ordinal()'));
test=test.replaceAll('ResourceExpansionEleven','ResourceExpansionTwelve')
 .replace(/var species=new CropSpecies\[\]\{.*?\};/,'var species=new CropSpecies[]{'+names.map(n=>'CropSpecies.'+up(n)).join(',')+'};')
 .replace(/var blocks=new Block\[\]\{.*?\};/,'var blocks=new Block[]{'+blocks.map(n=>'Blocks.'+n).join(',')+'};')
 .replace(/float\[\] speeds=\{.*?\}/,'float[] speeds={'+speeds.join(',')+'}')
 .replaceAll('>=CropSpecies.WHITE_DYE.ordinal()','>=CropSpecies.STICK.ordinal()')
 .replace('species==CropSpecies.HEART_OF_THE_SEA||species==CropSpecies.NETHER_STAR||species==CropSpecies.DRAGON_HEAD||species==CropSpecies.HEAVY_CORE',names.slice(8).map(n=>'species==CropSpecies.'+up(n)).join('||'));
fs.writeFileSync(t+'content/ResourceExpansionTwelveGameTests.java',test);
edit('src/guiPreview/java/com/agrievolution/preview/GrandmasJournalPreview.java',s=>s.replace('CropSpecies.WHITE_DYE.ordinal()','CropSpecies.STICK.ordinal()'));

