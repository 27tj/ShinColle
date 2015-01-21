package com.lulan.shincolle.handler;

import java.io.File;

import com.lulan.shincolle.reference.Reference;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.config.Configuration;


public class ConfigHandler {
	
	public static Configuration config;	//�ŧiconfig�ɹ���
	
	//�]�w���ܼ�
	//GENERAL
	public static boolean debugMode = false;
	//SHIP SETTING
	public static float hpRatio = 1.0f;

	
	//Ū���]�w�ɰѼ�
	private static void loadConfiguration() {
		//�O�_�}��debug mode (spam debug/info message)
		debugMode = config.getBoolean("Debug_Mode", "general", false, "Enable debug message (SPAM WARNING)");
		
		//Ū�� ship setting�]�w
		//hp ratio
		hpRatio = config.getFloat("Scale_HP", "ship setting", 1f, 0.1f, 10f, "Ship HP scale");	
		
		//�Y�]�w�ɦ���s�L �h�x�s
		if(config.hasChanged()) {
			config.save();
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
