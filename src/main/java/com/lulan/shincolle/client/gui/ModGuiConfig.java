package com.lulan.shincolle.client.gui;

import java.util.List;

import com.lulan.shincolle.handler.ConfigurationHandler;
import com.lulan.shincolle.reference.Reference;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;

public class ModGuiConfig extends GuiConfig {

	public ModGuiConfig(GuiScreen parentScreen) {
		
		//�Ѽƨ̧Ǭ�: �]�w�ɤ�����,�n�]�w���Ѽ�,mod id,�O�_�ݭn���Ұʦa��,�O�_�ݭn���Ұ�MC,���D
		super(parentScreen, 
				new ConfigElement(ConfigurationHandler.configuration.getCategory("ship setting")).getChildElements(),
				Reference.MOD_ID, 
				false,
				false, 
				"Ship Setting");
		// TODO Auto-generated constructor stub
	}

}
