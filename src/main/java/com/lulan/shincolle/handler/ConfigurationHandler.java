package com.lulan.shincolle.handler;

import java.io.File;

import com.lulan.shincolle.reference.Reference;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.config.Configuration;


public class ConfigurationHandler {
	
	public static Configuration configuration;	//�ŧiconfig�ɹ���
	
	//�]�w���ܼ�
	public static float hpRatio = 1.0f;

	
	//Ū���]�w�ɰѼ�
	private static void loadConfiguration() {
		//Ū�� ship setting�]�w
		//hp ratio
		hpRatio = configuration.getFloat("HP_Ratio", "ship setting", 1f, 0.1f, 10f, "Ship HP ratio");	
		
		//�Y�]�w�ɦ���s�L �h�x�s
		if(configuration.hasChanged()) {
			configuration.save();
		}		
	}
	
	
	//�]�w�ɳB�z ��l�ưʧ@
	public static void init(File configFile) {		
		//�p�G�]�w�ɹ����٥��إ� �h�إߤ�
		if(configuration == null) {
			configuration = new Configuration(configFile);	//�إ�config�ɹ���
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
