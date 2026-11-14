const fs=require('fs'),j='src/main/java/com/agrievolution/',t='src/test/java/com/agrievolution/';
const {names,field,lab}=require('./resource10-spec.json'),up=n=>n.toUpperCase(),title=n=>n.split('_').map(w=>w[0].toUpperCase()+w.slice(1)).join(' ');
const blocks=["GREEN_TERRACOTTA","RED_TERRACOTTA","YELLOW_TERRACOTTA","BROWN_TERRACOTTA","HONEY_BLOCK","MOSS_BLOCK","BLUE_TERRACOTTA","CYAN_TERRACOTTA","MAGMA_BLOCK","TUFF","END_STONE","EMERALD_BLOCK"];
const speeds=[2,2,2,2,3,3,3,3,4,4,5,5];
const edit=(p,f)=>fs.writeFileSync(p,f(fs.readFileSync(p,'utf8')));
if(fs.readFileSync(j+'farming/ResourceCropRules.java','utf8').includes('case GREEN_DYE ->'))throw Error('Already applied');
edit(j+'genetics/CropSpecies.java',s=>s.replace('<= WARPED_FUNGUS.ordinal()','<= EXPERIENCE_BOTTLE.ordinal()'));
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
edit(t+'genetics/ResourceProgressionSelfTest.java',s=>s.replace('public static void main(String[] args) {','public static void main(String[] args) {\n'+[['field',field],['lab',lab]].flatMap(([fn,rs])=>rs.map(([a,b,c,p])=>'        '+fn+'(CropSpecies.'+a+',CropSpecies.'+b+',CropSpecies.'+c+','+p+');')).join('\n')).replace('count()!=121','count()!=133').replace('CropSpecies.PEARLESCENT_FROGLIGHT.ordinal()!=142','CropSpecies.PEARLESCENT_FROGLIGHT.ordinal()!=142||CropSpecies.WARPED_FUNGUS.ordinal()!=154'));
edit(t+'genetics/SpeciesSmokeTest.java',s=>s.replace('length != 155','length != 167'));
edit(t+'client/journal/JournalNavigationSelfTest.java',s=>s.replaceAll('CropSpecies.WARPED_FUNGUS','CropSpecies.EXPERIENCE_BOTTLE').replace('== 39','== 42').replace('thirty-nine species','forty-two species').replaceAll('==152','==164').replaceAll('<=152','<=164').replace('all 150 crosses','all 162 crosses').replace('==100','==108').replace('==50','==54').replace('all 155 breeding','all 167 breeding'));
let test=fs.readFileSync(t+'content/ResourceExpansionNineGameTests.java','utf8');
edit(t+'content/ResourceExpansionNineGameTests.java',s=>s.replaceAll('>=CropSpecies.SUGAR.ordinal()','>=CropSpecies.SUGAR.ordinal()&&species.ordinal()<=CropSpecies.WARPED_FUNGUS.ordinal()'));
test=test.replaceAll('ResourceExpansionNine','ResourceExpansionTen')
 .replace(/var species=new CropSpecies\[\]\{.*?\};/,'var species=new CropSpecies[]{'+names.map(n=>'CropSpecies.'+up(n)).join(',')+'};')
 .replace(/var blocks=new Block\[\]\{.*?\};/,'var blocks=new Block[]{'+blocks.map(n=>'Blocks.'+n).join(',')+'};')
 .replace(/float\[\] speeds=\{.*?\}/,'float[] speeds={'+speeds.join(',')+'}')
 .replaceAll('>=CropSpecies.SUGAR.ordinal()','>=CropSpecies.GREEN_DYE.ordinal()')
 .replace('species==CropSpecies.WITHER_ROSE||species==CropSpecies.CHORUS_FLOWER||species==CropSpecies.CRIMSON_FUNGUS||species==CropSpecies.WARPED_FUNGUS',names.slice(8).map(n=>'species==CropSpecies.'+up(n)).join('||'));
fs.writeFileSync(t+'content/ResourceExpansionTenGameTests.java',test);
edit('src/guiPreview/java/com/agrievolution/preview/GrandmasJournalPreview.java',s=>s.replace('CropSpecies.SUGAR.ordinal()','CropSpecies.GREEN_DYE.ordinal()'));

