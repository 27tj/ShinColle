package com.lulan.shincolle.client.handler;

import com.lulan.shincolle.client.settings.KeyBindings;
import com.lulan.shincolle.reference.Key;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;

public class KeyInputEventHandler {
	
/*	//�������������Τ�k
	private static Key getPressedKeyBinding() {
		
		if(KeyBindings.repair.isPressed()) {
			return Key.REPAIR;
		}
		else if() {
			
		}
		return Key.UNKNOWN;	//��L�������� �^��UNKNOWN
	}*/
	
	//��������event �ھګ��䰵�X�^��
	@SubscribeEvent
	public void handleKeyInputEvent(InputEvent.KeyInputEvent event) {
	
		//debug��: ���U���� �bconsole��ܫ��U�ӫ���
	/*	LogHelper.info(KeyBindings.repair.isPressed());
		LogHelper.info(KeyBindings.repair.getIsKeyPressed());*/
		
	}

}
