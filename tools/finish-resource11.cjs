const fs=require('fs'),j='src/main/java/com/agrievolution/',t='src/test/java/com/agrievolution/';
const {names,field,lab}=require('./resource11-spec.json'),up=n=>n.toUpperCase(),title=n=>n.split('_').map(w=>w[0].toUpperCase()+w.slice(1)).join(' ');
const blocks=["WHITE_TERRACOTTA","BLACK_TERRACOTTA","ORANGE_TERRACOTTA","PINK_TERRACOTTA","PURPLE_TERRACOTTA","MAGENTA_TERRACOTTA","LIGHT_BLUE_TERRACOTTA","LIME_TERRACOTTA","DARK_PRISMARINE","SOUL_SAND","END_STONE","CHISELED_TUFF"];
const speeds=[2,2,2,2,3,3,3,3,4,5,5,5];
const edit=(p,f)=>fs.writeFileSync(p,f(fs.readFileSync(p,'utf8')));
if(fs.readFileSync(j+'farming/ResourceCropRules.java','utf8').includes('case WHITE_DYE ->'))throw Error('Already applied');
edit(j+'genetics/CropSpecies.java',s=>s.replace('<= EXPERIENCE_BOTTLE.ordinal()','<= HEAVY_CORE.ordinal()'));
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
edit(t+'genetics/ResourceProgressionSelfTest.java',s=>s.replace('public static void main(String[] args) {','public static void main(String[] args) {\n'+[['field',field],['lab',lab]].flatMap(([fn,rs])=>rs.map(([a,b,c,p])=>'        '+fn+'(CropSpecies.'+a+',CropSpecies.'+b+',CropSpecies.'+c+','+p+');')).join('\n')).replace('count()!=133','count()!=145').replace('CropSpecies.WARPED_FUNGUS.ordinal()!=154','CropSpecies.WARPED_FUNGUS.ordinal()!=154||CropSpecies.EXPERIENCE_BOTTLE.ordinal()!=166'));
edit(t+'genetics/SpeciesSmokeTest.java',s=>s.replace('length != 167','length != 179'));
edit(t+'client/journal/JournalNavigationSelfTest.java',s=>s.replaceAll('CropSpecies.EXPERIENCE_BOTTLE','CropSpecies.HEAVY_CORE').replace('== 42','== 45').replace('forty-two species','forty-five species').replaceAll('==164','==176').replaceAll('<=164','<=176').replace('all 162 crosses','all 174 crosses').replace('==108','==116').replace('==54','==58').replace('all 167 breeding','all 179 breeding'));
let test=fs.readFileSync(t+'content/ResourceExpansionTenGameTests.java','utf8');
edit(t+'content/ResourceExpansionTenGameTests.java',s=>s.replaceAll('>=CropSpecies.GREEN_DYE.ordinal()','>=CropSpecies.GREEN_DYE.ordinal()&&species.ordinal()<=CropSpecies.EXPERIENCE_BOTTLE.ordinal()'));
test=test.replaceAll('ResourceExpansionTen','ResourceExpansionEleven')
 .replace(/var species=new CropSpecies\[\]\{.*?\};/,'var species=new CropSpecies[]{'+names.map(n=>'CropSpecies.'+up(n)).join(',')+'};')
 .replace(/var blocks=new Block\[\]\{.*?\};/,'var blocks=new Block[]{'+blocks.map(n=>'Blocks.'+n).join(',')+'};')
 .replace(/float\[\] speeds=\{.*?\}/,'float[] speeds={'+speeds.join(',')+'}')
 .replaceAll('>=CropSpecies.GREEN_DYE.ordinal()','>=CropSpecies.WHITE_DYE.ordinal()')
 .replace('species==CropSpecies.FIRE_CHARGE||species==CropSpecies.WIND_CHARGE||species==CropSpecies.DRAGON_BREATH||species==CropSpecies.EXPERIENCE_BOTTLE',names.slice(8).map(n=>'species==CropSpecies.'+up(n)).join('||'));
fs.writeFileSync(t+'content/ResourceExpansionElevenGameTests.java',test);
edit('src/guiPreview/java/com/agrievolution/preview/GrandmasJournalPreview.java',s=>s.replace('CropSpecies.GREEN_DYE.ordinal()','CropSpecies.WHITE_DYE.ordinal()'));

