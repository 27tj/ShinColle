package com.lulan.shincolle.init;

import net.minecraftforge.common.MinecraftForge;

import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.handler.EVENT_BUS_EventHandler;
import com.lulan.shincolle.handler.FML_COMMON_EventHandler;

import cpw.mods.fml.common.FMLCommonHandler;

public class ModEvents {
	
	public static void init() {
		//�n���H�Uhandler��event bus�� �Ϩ�౵��event
		//FML bus
		FMLCommonHandler.instance().bus().register(new ConfigHandler());	 //config event handler
		FMLCommonHandler.instance().bus().register(new FML_COMMON_EventHandler());
		//	FMLCommonHandler.instance().bus().register(new KeyInputEventHandler());  //key event handler

		//EVENT bus
		MinecraftForge.EVENT_BUS.register(new EVENT_BUS_EventHandler());
		
		
		
	}

}
