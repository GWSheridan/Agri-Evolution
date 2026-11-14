const fs=require('fs');const names=['cultivated','nutrient','mythic','arcane','celestial'];const base='src/main/resources/assets/agri_evolution/';
for(const n of names)for(const state of ['dry','wet']){const path=base+'models/block/'+n+'_farmland_'+state+'.json';let model=JSON.parse(fs.readFileSync(path));model.textures.top='agri_evolution:block/'+n+'_farmland_'+state;model.textures.particle='agri_evolution:block/'+n+'_farmland_side';delete model.textures.accent;model.elements=model.elements.slice(0,1);model.elements[0].faces.up.uv=[0,0,16,16];for(const side of ['north','south','east','west'])model.elements[0].faces[side].uv=[0,0,16,15];fs.writeFileSync(path,JSON.stringify(model,null,2)+'\n');}
let p='gradle.properties';fs.writeFileSync(p,fs.readFileSync(p,'utf8').replace('mod_version=0.67.0','mod_version=0.67.1'));
p='build.gradle';fs.writeFileSync(p,fs.readFileSync(p,'utf8').replace("if (project.hasProperty('basinPreview'))", "if (project.hasProperty('farmlandPreview')) systemProperty 'agri.farmlandPreview', 'true'\n            if (project.hasProperty('basinPreview'))"));
p='src/guiPreview/java/com/agrievolution/preview/IrrigationModelPreview.java';let s=fs.readFileSync(p,'utf8').replace('(Boolean.getBoolean("agri.basinPreview")?"wooden-basins-":','(Boolean.getBoolean("agri.farmlandPreview")?"farmland-v2-":Boolean.getBoolean("agri.basinPreview")?"wooden-basins-":').replace('if(Boolean.getBoolean("agri.basinPreview")){renderBasins(g);return;}','if(Boolean.getBoolean("agri.farmlandPreview")){renderFarmland(g);return;}\n            if(Boolean.getBoolean("agri.basinPreview")){renderBasins(g);return;}');
s=s.replace('        private void renderBasins(GuiGraphics g){',`        private void renderFarmland(GuiGraphics g){
            g.fill(0,0,width,height,0xFFB9C4AF);
            var soils=new net.minecraft.world.level.block.Block[]{AEBlocks.CULTIVATED_FARMLAND.get(),AEBlocks.NUTRIENT_FARMLAND.get(),AEBlocks.MYTHIC_FARMLAND.get(),AEBlocks.ARCANE_FARMLAND.get(),AEBlocks.CELESTIAL_FARMLAND.get()};
            g.drawCenteredString(font,scene==0?"Dry soil - Cultivated / Nutrient / Mythic / Arcane / Celestial":scene==1?"Watered soil - all five tiers":"Joined farmland - furrows and side faces",width/2,18,0xFF203B27);
            g.flush();Lighting.setupFor3DItems();
            if(scene<2){for(int i=0;i<5;i++){
                var pose=g.pose();pose.pushPose();pose.translate((i+.5)*width/5.0,height/2.0+35,150);pose.scale(60,-60,60);pose.mulPose(Axis.XP.rotationDegrees(30));pose.mulPose(Axis.YP.rotationDegrees(-28));
                draw(g,soils[i].defaultBlockState().setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE,scene==0?0:7),0,0,0);pose.popPose();
            }}else{
                var pose=g.pose();pose.pushPose();pose.translate(width/2.0,height/2.0+42,150);pose.scale(52,-52,52);pose.mulPose(Axis.XP.rotationDegrees(40));pose.mulPose(Axis.YP.rotationDegrees(-20));
                for(int x=-2;x<=2;x++)for(int z=-1;z<=1;z++)draw(g,soils[scene==2?x+2:scene==3?0:4].defaultBlockState().setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE,7),x,0,z);
                pose.popPose();
            }Lighting.setupForFlatItems();
        }
        private void renderBasins(GuiGraphics g){`);fs.writeFileSync(p,s);
