package com.lulan.shincolle.client.gui;

import org.lwjgl.opengl.GL11;

import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.tileentity.TileEntitySmallShipyard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;

public class GuiSmallShipyard extends GuiContainer {

	private static final ResourceLocation guiTexture = new ResourceLocation(Reference.TEXTURES_GUI+"GuiSmallShipyard.png");
	private TileEntitySmallShipyard teSmallShipyard;
	
	public GuiSmallShipyard(InventoryPlayer par1, TileEntitySmallShipyard par2) {
		super(new ContainerSmallShipyard(par1, par2));
		teSmallShipyard = par2;
		
		this.xSize = 176;
		this.ySize = 164;
	}
	
	//GUI�e��: ��r 
	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		//���ogui��ܦW��
		String name = this.teSmallShipyard.hasCustomInventoryName() ? this.teSmallShipyard.getInventoryName() : I18n.format(this.teSmallShipyard.getInventoryName());
		String time = this.teSmallShipyard.getBuildTimeString();
		
		//�e�X�r�� parm: string, x, y, color, (�O�_dropShadow)
		//�e�X�Ӥ���W��, ��m: x=gui�e�ת��@�b�����r����פ@�b, y=6, �C�⬰4210752
		this.fontRendererObj.drawString(name, this.xSize / 2 - this.fontRendererObj.getStringWidth(name) / 2, 6, 4210752);
		//�e�X���a�I�]�W��
		this.fontRendererObj.drawString(I18n.format("container.inventory"), 8, this.ySize - 87, 4210752);
		//�e�X�˼Ʈɶ�
		this.fontRendererObj.drawString(time, 51, 51, 4210752);
		
	}

	//GUI�I��: �I���Ϥ�
	@Override
	protected void drawGuiContainerBackgroundLayer(float par1,int par2, int par3) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);	//RGBA
        Minecraft.getMinecraft().getTextureManager().bindTexture(guiTexture); //GUI����
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);	//GUI�j�p�]�w
       
        int scaleBar;
        //�e�Xbuild�i�ױ�          
        scaleBar = teSmallShipyard.getBuildProgressScaled(24);	//�m��i�ױ�����24
        drawTexturedModalRect(guiLeft+113, guiTop+29, 176, 0, scaleBar+1, 16);
        
        //�e�Xfuel�s�q��
        if(teSmallShipyard.remainedPower > 0) {
            scaleBar = teSmallShipyard.getPowerRemainingScaled(31);	//�m��i�ױ�����31	
            drawTexturedModalRect(guiLeft+10, guiTop+48-scaleBar, 176, 46-scaleBar, 12, scaleBar);
        }
	
	}

}
