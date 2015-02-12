package com.lulan.shincolle.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.reference.Reference;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;

public class ModGuiConfig extends GuiConfig {

	public ModGuiConfig(GuiScreen parentScreen) {
		
		//�Ѽƨ̧Ǭ�: �]�w�ɤ�����,�n�]�w���Ѽ�,mod id,�O�_�ݭn���Ұʦa��,�O�_�ݭn���Ұ�MC,���D
		super(parentScreen, 
				getConfigElements(),
				Reference.MOD_ID, 
				false,
				false, 
				GuiConfig.getAbridgedConfigPath(ConfigHandler.config.toString()));
		
	}
	
	private static List<IConfigElement> getConfigElements() {
	    List<IConfigElement> list = new ArrayList();
	    
	    list.addAll(new ConfigElement(ConfigHandler.config.getCategory("general")).getChildElements());
	    list.addAll(new ConfigElement(ConfigHandler.config.getCategory("ship setting")).getChildElements());

	    return list;
	}

}
