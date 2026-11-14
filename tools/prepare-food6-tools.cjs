const fs=require('fs');
let s=fs.readFileSync('tools/finish-food-expansion5.cjs','utf8');
s=s.replace("require('./food5-spec.json')","require('./food6-spec.json')");
s=s.slice(0,s.indexOf('const meals=['))+"const meals=require('./food6-meals.json');\n"+s.slice(s.indexOf("const edit="));
s=s.replace("includes('green_garden_broth')","includes('braised_garden_greens')");
const pages=[
 ['GREENS AND PULSES','FOOD GARDEN VI',['Spinach + Cabbage:','Kale (26%).','Kale + Lettuce:','Pak Choi (24%).','Cabbage + Cauliflower:','Brussels Sprouts (22%).'],['Turnip + Sweet Potato:','Swede (24%).','Celery + Turnip:','Celeriac (24%).','Peas + Sesame:','Lentil (24%).']],
 ['GRAINS AND FRUIT','FOOD GARDEN VI',['Beans + Peanut:','Chickpea (24%).','Wild Wheat + Oats:','Barley (26%).','Barley + Amber Marigold:','Buckwheat (22%).'],['Celery + Cranberry:','Rhubarb (22%).','Blackcurrant + Cucumber:','Grape (22%).','Pumpkin + Gooseberry:','Cantaloupe (22%).']],
 ['GARDEN KITCHEN VI','NEW HARVEST MEALS',['All twelve crops grow','on ordinary farmland.','Wait until stage 7.','Each plant uses one cell.','Greenhouses can harvest','and replant every species.'],['Try Braised Garden Greens,','Beef & Lentil Stew,','Chickpea Grain Bowl or','Summer Fruit Compote.','Recipes are pictured here.','Bowl meals return a bowl.']]
];
s=s.slice(0,s.indexOf('const pages=['))+'const pages='+JSON.stringify(pages)+';\n'+s.slice(s.indexOf("edit(j+'client/journal/MachineGuidePages.java'"));
s=s.replace("s=>s.replace('private static final Page[] PAGES = {'","s=>s.replace('28 HOMEMADE DISHES','32 HOMEMADE DISHES').replace('private static final Page[] PAGES = {'");
fs.writeFileSync('tools/finish-food-expansion6.cjs',s);
for(const f of ['prepare-sprites.ps1','prepare-meals.ps1','collect-journal-qa.ps1'])fs.writeFileSync('artwork/food-crops-v6/'+f,fs.readFileSync('artwork/food-crops-v5/'+f,'utf8').replaceAll('food5','food6'));
let p=fs.readFileSync('tools/verify-food5-preservation.ps1','utf8').replaceAll('food-expansion5','food-expansion6').replaceAll('1980','2104').replaceAll('56','60').replaceAll('0.63.0','0.64.0').replaceAll('203','215').replace('newSpecies.Count -ne 215','newSpecies.Count -ne 227').replaceAll('0630','0640');
// Do not change the SHA256 algorithm while changing the old recipe count.
p=p.replaceAll('SHA260','SHA256');
fs.writeFileSync('tools/verify-food6-preservation.ps1',p);
