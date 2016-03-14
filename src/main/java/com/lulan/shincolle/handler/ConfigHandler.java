package com.lulan.shincolle.handler;

import java.io.File;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;


public class ConfigHandler {
	
	public static Configuration config;		//�ŧiconfig�ɹ���
	
	//�]�w���ܼ�
	//GENERAL
	public static boolean debugMode = false;
	public static boolean easyMode = false;
	public static boolean staticMode = false;
	public static boolean showTag = true;
	public static boolean friendlyFire = true;
	public static boolean useWakamoto = true;
	public static boolean alwaysShowTeamParticle = false;
	public static boolean polyAsMn = true;
	public static float dropGrudge = 1.0F;
	public static int closeGUIDist = 64;
	public static int bossCooldown = 4800;
	public static int teamCooldown = 6000;
	public static int kaitaiAmountSmall = 20;
	public static int kaitaiAmountLarge = 20;
	
	//DESK
	public static int radarUpdate = 128;	//radar update interval (ticks)
	
	//SHIP SETTING
	public static Property propShip, propShipLimitBasic, propShipLimitEffect,
						   propBossSmall, propBossLarge, propMobSubm, propGrudgeShip, propGrudgeAction;
	//                                                    HP, ATK, DEF, SPD, MOV, HIT
	public static double[] limitShipBasic = new double[] {-1D, -1D, 75D, 4D, 0.6D, 64D};
	//                                                    CRI, DHIT, THIT, MISS, AA, ASM, DODGE
	public static double[] limitShipEffect = new double[] {-1D, -1D, -1D, -1D, -1D, -1D, 75D};
	public static double[] scaleShip = new double[] {1D, 1D, 1D, 1D, 1D, 1D};
	//													  HP, ATK, DEF, SPD, MOV, HIT
	public static double[] scaleBossSmall = new double[] {2000D, 120D, 80D, 1D, 0.6D, 20D};
	public static double[] scaleBossLarge = new double[] {5000D, 200D, 92D, 2D, 0.36D, 24D};
	//	  												HP, ATK, DEF, SPD, MOV, HIT, spawnPerSquid
	public static double[] scaleMobSubm = new double[] {200D, 36D, 20D, 0.8D, 0.3D, 12D, 200D};
	//grudge consumption:                              DD CL CA CAV CLT CVL CV BB BBV SS AP 
	public static int[] consumeGrudgeShip = new int[] {5, 7, 8, 9,  8,  11, 12,15,14, 4, 3};
	//grudge consumption:                                LAtk, HAtk, LAir, HAir, moving
	public static int[] consumeGrudgeAction = new int[] {2,    6,    3,    9,    2};
	
	public static int dmgSvS = 100;		//ship vs ship damage modifier, 20 = dmg * 20%
	public static int dmgSummon = 100;	//summons damage modifier, 20 = dmg * 20%
	public static int expMod = 20;		//ship exp per level, ex: 20 => lv 15 exp req = 15*20+20
	
	public static boolean timeKeeping = true;
	public static float timeKeepingVolume = 1.0F;
	public static float shipVolume = 1.0F;
	public static float fireVolume = 0.7F;
	
	//WORLD GEN
	public static int polyOreBaseRate = 7;
	public static int polyGravelBaseRate = 4;
	public static boolean[] polyGravelBaseBlock = new boolean[] {true, true, false, false};	//stone gravel sand dirt
	public static Property propPolyGravel;
	
	
	//Ū���]�w�ɰѼ�
	private static void loadConfiguration() {
		
		//�O�_���custom name tag
		alwaysShowTeamParticle = config.getBoolean("Always_Show_Team", "general", false, "Always show team circle animation");
		
		//boss�ͦ�cd�]�w (ticks)
		bossCooldown = config.getInt("Boss_Cooldown", "general", 4800, 20, 1728000, "Boss spawn cooldown");
		
		//���a���}�h��������GUI
		closeGUIDist = config.getInt("Close_GUI_Distance", "general", 64, 2, 64, "Close inventory GUI if ship away from player X blocks");
		
		//�O�_�}��debug mode (spam debug/info message)
		debugMode = config.getBoolean("Debug_Mode", "general", false, "Enable debug message (SPAM WARNING)");
		
		//grudge�����v�]�w
		dropGrudge = config.getFloat("DropRate_Grudge", "general", 1F, 0F, 64F, "Grudge drop rate (ex: 0.5 = 50% drop 1 grudge, 5.5 = drop 5 grudge + 50% drop 1 grudge)");
		
		//�O�_�}��²��Ҧ� (spam debug/info message)
		easyMode = config.getBoolean("Easy_Mode", "general", false, "Easy mode: decrease Large Construction resources requirement, increase ammo / grudge gained from items");
		
		//�O�_�}��²��Ҧ� (spam debug/info message)
		friendlyFire = config.getBoolean("Friendly_Fire", "general", true, "false: disable damage done by player (except owner)");
			
		//������o���ƶq�]�w, mob drop��ship���w
		kaitaiAmountSmall = config.getInt("Recycle_Small", "general", 20, 0, 1000, "Recycle amount by Dismantle Hammer (SMALL Mob Drop Ship)");
		kaitaiAmountLarge = config.getInt("Recycle_Large", "general", 20, 0, 1000, "Recycle amount by Dismantle Hammer (LARGE Mob Drop Ship)");
				
		//�O�_��h���ݷ����q
		polyAsMn = config.getBoolean("Polymetal_as_Mn", "general", true, "true: Polymetallic Nodules = Manganese Dust, Polymetallic Ore = Manganese Ore");
		
		//desk�p�F��s���j
		radarUpdate = config.getInt("Radar_Update", "desk", 128, 20, 6000, "Radar update interval (ticks) in Admiral's Desk GUI");
		
		//�O�_��large shipyard�]��static entity (�u�e�@��, ���O���\���NEI�۽�)
		staticMode = config.getBoolean("Static_Mode", "general", false, "Render LargeShipyard as static or normal entity (for NotEnoughItem: 1283: Stack overflow bug)");
		
		//�O�_���custom name tag
		showTag = config.getBoolean("Show_Name_Tag", "general", true, "Show custom name tag?");
		
		//team���cd (ticks)
		teamCooldown = config.getInt("Battle_Cooldown", "general", 6000, 20, 1728000, "Create/Disband Team Cooldown");

		//�O�_�}��²��Ҧ� (spam debug/info message)
		useWakamoto = config.getBoolean("Sound_Wakamoto", "general", true, "enable Wakamoto sound for particular ship");
		
		//Ū�� ship setting�]�w
		timeKeeping = config.getBoolean("Timekeeping", "ship setting", true, "Play timekeeping sound every 1000 ticks (1 minecraft hour)");
		timeKeepingVolume = config.getFloat("Timekeeping_Volume", "ship setting", 1.0F, 0F, 10F, "Timekeeping sound volume");
		shipVolume = config.getFloat("Ship_Volume", "ship setting", 1.0F, 0F, 10F, "Other sound volume");
		fireVolume = config.getFloat("Attack_Volume", "ship setting", 0.7F, 0F, 10F, "Attack sound volume");
		
		propShip = config.get("ship setting", "ship_scale", scaleShip, "Ship attributes SCALE: HP, firepower, armor, attack speed, move speed, range");
		propShipLimitBasic = config.get("ship setting", "ship_limit_basic", limitShipBasic, "Ship basic attributes LIMIT (-1 = no limit): HP, firepower, armor%, attack speed, move speed, range(blocks)");
		propShipLimitEffect = config.get("ship setting", "ship_limit_effect", limitShipEffect, "Ship effect attributes LIMIT (-1 = no limit, 12 = limit 12%): critical%, double hit%, triple hit%, miss reduction%, anti-air, anti-ss, dodge%");
		propBossSmall = config.get("ship setting", "SmallBoss_scale", scaleBossSmall, "Small Boss:Shimakaze  Values: HP, firepower, armor, attack speed, move speed, range");
		propBossLarge = config.get("ship setting", "LargeBoss_scale", scaleBossLarge, "Large Boss:Nagato  Values: HP, firepower, armor, attack speed, move speed, range");
		propMobSubm = config.get("ship setting", "Mob_Submarine_scale", scaleMobSubm, "Submarine:U511/Ro500  Values: HP, firepower, armor, attack speed, move speed, range, spawnPerSquid");
		propGrudgeShip = config.get("ship setting", "Grudge_Ship", consumeGrudgeShip, "Grudge consumption for ship type: DD CL CA CAV CLT CVL CV BB BBV SS AP (MAX = 120)");
		propGrudgeAction = config.get("ship setting", "Grudge_Action", consumeGrudgeAction, "Grudge consumption for ship action: Light attack, Heavy attack, Light aircraft, Heavy aircraft, Moving per block");
		
		//ship vs ship damage modifier
		dmgSvS = config.getInt("SVS_DmgTaken", "ship setting", 100, 0, 10000, "Ship vs Ship damage modifier, 20 = damage * 20% ");
		dmgSummon = config.getInt("Summon_DmgTaken", "ship setting", 100, 0, 10000, "summons (mounts, aircraft ...etc) damage modifier, 20 = damage * 20% ");
		expMod = config.getInt("EXP_Modifier", "ship setting", 20, 1, 10000, "ship experience modifier, 20 = level 150: 150*20+20 = 3020");
		
		//WORLD GEN
		polyOreBaseRate = config.getInt("Polymetal_Ore", "world gen", 7, 0, 100, "Polymetallic Ore clusters in one chunk");
		polyGravelBaseRate = config.getInt("Polymetal_Gravel", "world gen", 4, 0, 100, "Polymetallic Gravel clusters in one chunk");
		propPolyGravel = config.get("world gen", "Polymetal_Gravel_Replace", polyGravelBaseBlock, "PolyGravel replaced block: stone, gravel, sand, dirt", true, 4);
		
		//�]�w�s��
		limitShipBasic = getDoubleArrayFromConfig(limitShipBasic, propShipLimitBasic);
		limitShipEffect = getDoubleArrayFromConfig(limitShipEffect, propShipLimitEffect);
		scaleShip = getDoubleArrayFromConfig(scaleShip, propShip);
		scaleBossSmall = getDoubleArrayFromConfig(scaleBossSmall, propBossSmall);
		scaleBossLarge = getDoubleArrayFromConfig(scaleBossLarge, propBossLarge);
		scaleMobSubm = getDoubleArrayFromConfig(scaleMobSubm, propMobSubm);
		polyGravelBaseBlock = getBooleanArrayFromConfig(polyGravelBaseBlock, propPolyGravel);
		consumeGrudgeShip = getIntArrayFromConfig(consumeGrudgeShip, propGrudgeShip);
		consumeGrudgeAction = getIntArrayFromConfig(consumeGrudgeAction, propGrudgeAction);
		
		//�Y�]�w�ɦ���s�L, �h�x�s
		if(config.hasChanged()) {
			config.save();
		}
	}
	
	//check get value
	public static int[] getIntArrayFromConfig(int[] defaultValue, Property target) {
		int size = defaultValue.length;
		int[] geti = target.getIntList();
		
		if(geti != null && geti.length == size) {
			return geti;
		}
		else {
			target.set(defaultValue);
			return defaultValue;
		}
	}
	
	//check get value
	public static double[] getDoubleArrayFromConfig(double[] defaultValue, Property target) {
		int size = defaultValue.length;
		double[] getd = target.getDoubleList();
		
		if(getd != null && getd.length == size) {
			return getd;
		}
		else {
			target.set(defaultValue);
			return defaultValue;
		}
	}
	
	//check get value
	public static boolean[] getBooleanArrayFromConfig(boolean[] defaultValue, Property target) {
		int size = defaultValue.length;
		boolean[] getd = target.getBooleanList();
		
		if(getd != null && getd.length == size) {
			return getd;
		}
		else {
			target.set(defaultValue);
			return defaultValue;
		}
	}
	
	//�]�w�ɳB�z ��l�ưʧ@
	public static void init(File configFile) {		
		//�p�G�]�w�ɹ����٥��إ� �h�إߤ�
		if(config == null) {
			config = new Configuration(configFile);	//�إ�config�ɹ���
			loadConfiguration();
		}
	}
	
	//�Y������s�� �]�w�ɻݭn��s �h�b���϶��W�[��s��k
	@SubscribeEvent
	public void onConfigurationChangedEvent(ConfigChangedEvent.OnConfigChangedEvent event) {
		//�Y�]�w�ɪ�mod id��ثemod id���P�� �h�i���s
		if(event.modID.equalsIgnoreCase(Reference.MOD_ID)) {
			loadConfiguration();
		}
	}

}
