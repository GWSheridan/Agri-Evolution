const fs=require('fs');
let s=fs.readFileSync('tools/verify-resource8.cjs','utf8')
 .replaceAll('resource8-spec','resource9-spec').replaceAll('resources8-','resources9-')
 .replaceAll('0560','0570').replaceAll('0.56.0','0.57.0')
 .replaceAll('1144','1240').replaceAll('1404','1512').replaceAll('1296','1404')
 .replaceAll('143','155').replaceAll('resourceCrops:109','resourceCrops:121')
 .replaceAll('83 required','86 required').replaceAll('Passed:83','Passed:86').replaceAll('83 GameTests','86 GameTests')
 .replaceAll('Recipes:140','Recipes:152').replaceAll('Recipes:92','Recipes:100').replaceAll('Recipes:46','Recipes:50');
fs.writeFileSync('tools/verify-resource9.cjs',s);
fs.copyFileSync('artwork/resource-expansion-v8/journal-contact-sheet.ps1','artwork/resource-expansion-v9/journal-contact-sheet.ps1');

