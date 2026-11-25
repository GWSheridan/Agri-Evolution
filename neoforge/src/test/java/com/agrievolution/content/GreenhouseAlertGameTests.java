package com.agrievolution.content;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class GreenhouseAlertGameTests {
 @GameTest(template="empty") public static void alertsReflectSyncedState(GameTestHelper h){
  var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var m=new BotanicalGreenhouseMenu(1,player.getInventory());
  m.setData(2,1);m.setData(3,1);m.setData(12,23);m.setData(13,60);m.setData(19,1);m.setData(20,1);m.setData(25,1);m.setData(26,1);m.setData(27,1);
  var alerts=GreenhouseStatus.alerts(m);
  for(String name:java.util.List.of("Water","Storage","Seeds","Automation"))h.assertTrue(alerts.stream().anyMatch(a->a.name().equals(name)&&a.severity()==2),"Expected actionable alert: "+name);
  m.setData(0,4000);m.setData(25,0);m.setData(28,20);m.setData(38,1);
  h.assertTrue(GreenhouseStatus.alerts(m).isEmpty(),"Warnings clear when synchronized conditions recover");
  h.assertTrue(GreenhouseStatus.targetTab("Water")==1&&GreenhouseStatus.targetTab("Seeds")==6,"Shortcuts target relevant controls");h.succeed();
 }
}
