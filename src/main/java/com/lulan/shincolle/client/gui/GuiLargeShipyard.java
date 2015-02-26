package com.lulan.shincolle.client.gui;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import scala.reflect.internal.Trees.This;

import com.lulan.shincolle.client.inventory.ContainerLargeShipyard;
import com.lulan.shincolle.network.C2SGUIPackets;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.tileentity.TileMultiGrudgeHeavy;
import com.lulan.shincolle.utility.GuiHelper;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;

/**SLOT POSITION
 * output(168,51) fuel bar(9,83 height=63) fuel color bar(208,64)
 * ship button(157,24) equip button(177,24) inv(25,116)
 * player inv(25,141) action bar(25,199)
 */
public class GuiLargeShipyard extends GuiContainer {

	private static final ResourceLocation TEXTURE_BG = new ResourceLocation(Reference.TEXTURES_GUI+"GuiLargeShipyard.png");
	private TileMultiGrudgeHeavy tile;
	private int xClick, yClick, selectMat, buildType, invMode, xMouse, yMouse;
	private String name, time, errorMsg, matBuild0, matBuild1, matBuild2, matBuild3, matStock0, matStock1, matStock2, matStock3;
	
	public GuiLargeShipyard(InventoryPlayer par1, TileMultiGrudgeHeavy par2) {
		super(new ContainerLargeShipyard(par1, par2));
		this.tile = par2;
		this.xSize = 208;
		this.ySize = 223;
	}
	
	//get new mouseX,Y and redraw gui
	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);
		xMouse = mouseX;
		yMouse = mouseY;
	}
	
	//GUI�e��: ��r 
	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		//���ogui��ܦW��
		name = I18n.format("container.shincolle:LargeShipyard");
		time = this.tile.getBuildTimeString();
		matBuild0 = String.valueOf(this.tile.getMatBuild(0));
		matBuild1 = String.valueOf(this.tile.getMatBuild(1));
		matBuild2 = String.valueOf(this.tile.getMatBuild(2));
		matBuild3 = String.valueOf(this.tile.getMatBuild(3));
		matStock0 = String.valueOf(this.tile.getMatStock(0));
		matStock1 = String.valueOf(this.tile.getMatStock(1));
		matStock2 = String.valueOf(this.tile.getMatStock(2));
		matStock3 = String.valueOf(this.tile.getMatStock(3));
		
		//�e�X�r�� parm: string, x, y, color, (�O�_dropShadow)
		//�e�X�Ӥ���W��, ��m: x=gui�e�ת��@�b�����r����פ@�b, y=6, �C�⬰4210752
		this.fontRendererObj.drawString(name, this.xSize / 2 - this.fontRendererObj.getStringWidth(name) / 2, 6, 4210752);
		//�e�X�˼Ʈɶ�
		this.fontRendererObj.drawString(time, 176 - this.fontRendererObj.getStringWidth(time) / 2, 77, 4210752);
		//�e�X���ܰT��
		if(tile.getPowerGoal() <= 0 && tile.getBuildType() != 0) {
			errorMsg = I18n.format("gui.shincolle:nomaterial");
			this.fontRendererObj.drawString(errorMsg, 105 - this.fontRendererObj.getStringWidth(errorMsg) / 2, 99, 16724787);
		}
		else if(!tile.hasPowerRemained()) {
			errorMsg = I18n.format("gui.shincolle:nofuel");
			this.fontRendererObj.drawString(errorMsg, 105 - this.fontRendererObj.getStringWidth(errorMsg) / 2, 99, 16724787);
		}
		//�e�X�Ʀr
		this.fontRendererObj.drawString(matBuild0, 73 - this.fontRendererObj.getStringWidth(matBuild0) / 2, 20, 16777215);
		this.fontRendererObj.drawString(matBuild1, 73 - this.fontRendererObj.getStringWidth(matBuild1) / 2, 39, 16777215);
		this.fontRendererObj.drawString(matBuild2, 73 - this.fontRendererObj.getStringWidth(matBuild2) / 2, 58, 16777215);
		this.fontRendererObj.drawString(matBuild3, 73 - this.fontRendererObj.getStringWidth(matBuild3) / 2, 77, 16777215);
		this.fontRendererObj.drawString(matStock0, 125 - this.fontRendererObj.getStringWidth(matStock0) / 2, 20, 16776960);
		this.fontRendererObj.drawString(matStock1, 125 - this.fontRendererObj.getStringWidth(matStock1) / 2, 39, 16776960);
		this.fontRendererObj.drawString(matStock2, 125 - this.fontRendererObj.getStringWidth(matStock2) / 2, 58, 16776960);
		this.fontRendererObj.drawString(matStock3, 125 - this.fontRendererObj.getStringWidth(matStock3) / 2, 77, 16776960);
	
		handleHoveringText();
	}

	//GUI�I��: �I���Ϥ�
	@Override
	protected void drawGuiContainerBackgroundLayer(float par1,int par2, int par3) {
//		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);	//RGBA
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE_BG); //GUI����
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);	//GUI�j�p�]�w
       
        //�e�Xfuel�s�q��
        int scaleBar; 
        if(tile.hasPowerRemained()) {
            scaleBar = tile.getPowerRemainingScaled(64);	//�m��i�ױ�����64	
            drawTexturedModalRect(guiLeft+9, guiTop+83-scaleBar, 208, 64-scaleBar, 12, scaleBar);
        }
        
        //�e�Xtype��ܮ� (157,24)
        if(tile.getBuildType() != 0) {
        	drawTexturedModalRect(guiLeft+137+tile.getBuildType()*20, guiTop+24, 208, 64, 18, 18);
        }
        
        //�e�X����ƶq���s (50,8)
        drawTexturedModalRect(guiLeft+50, guiTop+8+tile.getSelectMat()*19, 0, 223, 48, 30);
        
        //�e�X�����ܮ� (27,14)
        drawTexturedModalRect(guiLeft+27, guiTop+14+tile.getSelectMat()*19, 208, 64, 18, 18);
	
        //�e�Xinventory mode���s (23,92)
        if(tile.getInvMode() == 1) {	//iutput mode
        	drawTexturedModalRect(guiLeft+23, guiTop+92, 208, 82, 25, 20);
        }   
	}
	
	//draw tooltip
	private void handleHoveringText() {		
		//�e�Xfuel�s�q (8,19,22,84)
		if(xMouse > 8+guiLeft && xMouse < 22+guiLeft && yMouse > 19+guiTop && yMouse < 84+guiTop) {
			List list = new ArrayList();
			String strFuel = String.valueOf(tile.getPowerRemained());
			int strLen = this.fontRendererObj.getStringWidth(strFuel) / 2;
			list.add(strFuel);
			this.drawHoveringText(list, 3-strLen, 58, this.fontRendererObj);
//			this.fontRendererObj.drawStringWithShadow(strFuel, 25-strLen, 47, 16777215);
		}	
	}
	
	//handle mouse click, @parm posX, posY, mouseKey (0:left 1:right 2:middle 3:...etc)
	@Override
	protected void mouseClicked(int posX, int posY, int mouseKey) {
        super.mouseClicked(posX, posY, mouseKey);
            
        //get click position
        xClick = posX - this.guiLeft;
        yClick = posY - this.guiTop;
        
        //build type button
        buildType = this.tile.getBuildType();
        invMode = this.tile.getInvMode();
        selectMat = this.tile.getSelectMat(); 
        
        //page 0 button
        int buttonClicked = GuiHelper.getButton(ID.LARGESHIPYARD, 0, xClick, yClick);
        switch(buttonClicked) {
        case 0:	//build ship
        	if(buildType == 1) {
        		buildType = 0;	//�쥻�Iship �S�I�@�� �h�k0
        	}
        	else {
        		buildType = 1;	//�쥻�I��L���s, �h�]��ship
        	}
        	LogHelper.info("DEBUG : GUI click: build large ship: ship "+buildType);
        	CommonProxy.channel.sendToServer(new C2SGUIPackets(this.tile, ID.B_Shipyard_Type, buildType, 0));
        	break;
        case 1:	//build equip
        	if(buildType == 2) {
        		buildType = 0;	//�쥻�Iequip �S�I�@�� �h�k0
        	}
        	else {
        		buildType = 2;	//�쥻�I��L���s, �h�]��equip
        	}
        	LogHelper.info("DEBUG : GUI click: build large ship: equip "+buildType);
        	CommonProxy.channel.sendToServer(new C2SGUIPackets(this.tile, ID.B_Shipyard_Type, buildType, 0));
        	break;
        case 2:	//inventory mode
        	if(invMode == 0) {
        		invMode = 1;	//�쥻��input mode, �אּoutput mode
        	}
        	else {
        		invMode = 0;
        	}
        	LogHelper.info("DEBUG : GUI click: build large ship: invMode "+invMode);
        	CommonProxy.channel.sendToServer(new C2SGUIPackets(this.tile, ID.B_Shipyard_InvMode, invMode, 0));
        	break;
        case 3:	//select material grudge
        case 4: //abyssium
        case 5: //ammo
        case 6: //polymetal
        	selectMat = buttonClicked - 3;
        	LogHelper.info("DEBUG : GUI click: build large ship: select mats "+selectMat);
        	CommonProxy.channel.sendToServer(new C2SGUIPackets(this.tile, ID.B_Shipyard_SelectMat, selectMat, 0));
        	break;
        case 7:	//select material grudge num
        case 8: //abyssium num
        case 9: //ammo num
        case 10://polymetal num
        	selectMat = buttonClicked - 7;
        	LogHelper.info("DEBUG : GUI click: build large ship: select mats (num) "+selectMat);
        	CommonProxy.channel.sendToServer(new C2SGUIPackets(this.tile, ID.B_Shipyard_SelectMat, selectMat, 0));
        	break;
        }//end page 0 button switch
        
        //other page button
        buttonClicked = GuiHelper.getButton(ID.LARGESHIPYARD, selectMat+1, xClick, yClick);
        switch(buttonClicked) {
        case 0:	//build mat +1000
        case 1:	//build mat +100
        case 2:	//build mat +10
        case 3:	//build mat +1
        case 4:	//build mat -1000
        case 5:	//build mat -100
        case 6:	//build mat -10
        case 7:	//build mat -1
        	LogHelper.info("DEBUG : GUI click: build large ship: inc/dec build materials "+(selectMat+1)+" "+buttonClicked);
        	CommonProxy.channel.sendToServer(new C2SGUIPackets(this.tile, ID.B_Shipyard_INCDEC, selectMat, buttonClicked));
        	break;	
        }//end other page button switch
        
        
	}

}

