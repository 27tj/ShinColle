package com.lulan.shincolle.client.gui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.lulan.shincolle.client.gui.inventory.ContainerDesk;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.ExtendPlayerProps;
import com.lulan.shincolle.network.C2SGUIPackets;
import com.lulan.shincolle.proxy.ClientProxy;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.tileentity.TileEntityDesk;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.GuiHelper;
import com.lulan.shincolle.utility.LogHelper;

/** admiral's desk
 * 
 *  function:
 *  0: no function
 *  1: radar
 *  2: book
 * 
 *
 */
public class GuiDesk extends GuiContainer {

	private static final ResourceLocation guiTexture = new ResourceLocation(Reference.TEXTURES_GUI+"GuiDesk.png");
	private static final ResourceLocation guiRadar = new ResourceLocation(Reference.TEXTURES_GUI+"GuiDeskRadar.png");
	private static final ResourceLocation guiBook = new ResourceLocation(Reference.TEXTURES_GUI+"GuiDeskBook.png");
	private static final ResourceLocation guiTeam = new ResourceLocation(Reference.TEXTURES_GUI+"GuiDeskTeam.png");
	private static final ResourceLocation guiTarget = new ResourceLocation(Reference.TEXTURES_GUI+"GuiDeskTarget.png");
	
	private TileEntityDesk tile;
	private int xClick, yClick, xMouse, yMouse;
	private int tickGUI, guiFunc;
	private int[] listNum, listClicked; //list var: 0:radar 1:team 2:target
	private String errorMsg;
	
	//player data
	EntityPlayer player;
	ExtendPlayerProps extProps;
	
	//radar
	private int radar_zoomLv;			//0:256x256 1:64x64 2:16x16
	private List<ShipEntity> shipList;	//ship list
	
	//book
	private int book_chapNum;
	private int book_pageNum;
	
	//team
	private List<String> teamList;		//team list
	
	//target list
	Entity targetEntity = null;			//entity for model display
	private List<String> tarList;		//target list
	
	
	//object: ship entity + pixel position
	private class ShipEntity {
		public Entity ship;
		public String name;
		public double pixelx;	//included guiLeft/guiTop distance
		public double pixely;
		public double pixelz;
		
		public void setShip(Entity ship) {
			this.ship = ship;
			//get name
			if(((EntityLiving) ship).getCustomNameTag() != null && ((EntityLiving) ship).getCustomNameTag().length() > 0) {
				name = ((EntityLiving) ship).getCustomNameTag();
			}
			else {
				name = I18n.format("entity.shincolle."+ship.getClass().getSimpleName()+".name");
			}
		}
	}
	
	
	public GuiDesk(InventoryPlayer par1, TileEntityDesk par2) {
		super(new ContainerDesk(par1, par2));
		this.xSize = 256;
		this.ySize = 192;
		
		this.tile = par2;
		this.tickGUI = 0;				//ticks in gui (not game tick)
		
		//player data
		player = ClientProxy.getClientPlayer();
		extProps = (ExtendPlayerProps) player.getExtendedProperties(ExtendPlayerProps.PLAYER_EXTPROP_NAME);
		
		//list var: 0:radar 1:team 2:target
		this.listNum = new int[] {0, 0, 0};
		this.listClicked = new int[] {-1, -1, -1};
		
		//radar
		this.shipList = new ArrayList();
		
		//team
		this.teamList = new ArrayList();
		
		//target
		this.tarList = new ArrayList();
	}
	
	//get new mouseX,Y and redraw gui
	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);
		xMouse = mouseX;
		yMouse = mouseY;
		
		tickGUI += 1;
	}
	
	//draw tooltip
	private void handleHoveringText() {
		int mx = xMouse - guiLeft;
		int my = yMouse - guiTop;
		
		//draw ship name on light spot in radar function
		switch(this.guiFunc) {
		case 1:  /** radar */
			List list = new ArrayList();
			
			for(ShipEntity obj : this.shipList) {
				Entity ship = null;
				
				if(obj != null) {
					//mouse point at light spot icon
					if(xMouse < obj.pixelx+4 && xMouse > obj.pixelx-2 &&
					   yMouse < obj.pixelz+4 && yMouse > obj.pixelz-2) {
						ship = obj.ship;
					}
				}
				
				if(ship != null) {
					String strName = obj.name;
	    			list.add(strName);  //add string to draw list
				}
			}//end for all obj in shipList
			
			//draw string
			drawHoveringText(list, xMouse-guiLeft, yMouse-guiTop, this.fontRendererObj);
			break;  //end radar
		case 2:     /** book */
			int getbtn = GuiHelper.getButton(ID.G.ADMIRALDESK, 2, mx, my);

			//get chap text
        	if(getbtn > 1 && getbtn < 9) {
        		getbtn -= 2;
        		String strChap = I18n.format("gui.shincolle:book.chap"+getbtn+".title");
        		List list2 = CalcHelper.stringConvNewlineToList(strChap);
        		int strLen = this.fontRendererObj.getStringWidth(strChap);
        		drawHoveringText(list2, mx-strLen-20, my, this.fontRendererObj);
        	}
        	else {
        		int id = GuiBook.getIndexID(this.book_chapNum, this.book_pageNum);
        		List<int[]> cont = Values.BookList.get(id);
        		
        		if(cont != null) {
        			for(int[] getc : cont) {
        				if(getc != null && getc.length == 5) {
        					int xa = getc[2] + GuiBook.Page0LX;              //at left page
        					if(getc[1] > 0) xa = getc[2] + GuiBook.Page0RX;  //at right page
        					int xb = xa + 16;
        					int ya = getc[3] + GuiBook.Page0Y;
        					int yb = ya + 16;
        					ItemStack item = GuiBook.getItemStackForIcon(getc[4]);
        					
        					if(mx > xa && mx < xb && my > ya && my < yb) {
        						this.renderToolTip(item, mx, my);
        						break;
        					}
        				}
        			}//end for book content
        		}//end if book content
        	}
        	
			break;  //end book
//		case 3:		//team
//			break;	//end team
//		case 4:		//target
//			
//			break;	//end target
		}//end func switch
	}

	//GUI�e��: ��r 
	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		//draw ship data in radar function
		switch(this.guiFunc) {
		case 1:  //radar
			drawRadarText();
			break;  //end radar
		case 2:  //book
			GuiBook.drawBookContent(this, this.fontRendererObj, this.book_chapNum, this.book_pageNum);
			break;
		case 3:		//team
			drawTeamText();
			break;	//end team
		case 4:		//target
			drawTargetText();
			break;	//end target
		}
		
		//�e�Xtooltip
		handleHoveringText();
	}

	//GUI�I��: �I���Ϥ�
	@Override
	protected void drawGuiContainerBackgroundLayer(float par1,int par2, int par3) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);	//RGBA
        Minecraft.getMinecraft().getTextureManager().bindTexture(guiTexture); //GUI����
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
       
        //get new value
  		this.guiFunc = this.tile.guiFunc;
  		this.radar_zoomLv = this.tile.radar_zoomLv;
  		this.book_chapNum = this.tile.book_chap;
  		this.book_pageNum = this.tile.book_page;
      		
        //�e�X�\����s
        switch(this.guiFunc) {
        case 1:		//radar
        	drawTexturedModalRect(guiLeft+3, guiTop+2, 0, 192, 16, 16);
        	break;
        case 2:		//book
        	drawTexturedModalRect(guiLeft+22, guiTop+2, 16, 192, 16, 16);
        	break;
        case 3:		//team
        	drawTexturedModalRect(guiLeft+41, guiTop+2, 32, 192, 16, 16);
        	break;
        case 4:		//target
        	drawTexturedModalRect(guiLeft+60, guiTop+2, 48, 192, 16, 16);
        	break;
        }
        
        //�e�X�\�श��
        switch(this.guiFunc) {
        case 1:		//radar
        	//background
        	Minecraft.getMinecraft().getTextureManager().bindTexture(guiRadar);
        	drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        	
        	//draw zoom level button
        	int texty = 192;
        	switch(this.radar_zoomLv) {
        	case 1:
        		texty = 200;
        		break;
        	case 2:
        		texty = 208;
        		break;
        	}
        	drawTexturedModalRect(guiLeft+9, guiTop+160, 24, texty, 44, 8);
        	
        	//draw ship clicked circle
        	if(this.listClicked[0] > -1 && this.listClicked[0] < 5) {
        		int cirY = 25 + this.listClicked[0] * 32;
            	drawTexturedModalRect(guiLeft+142, guiTop+cirY, 68, 192, 108, 31);
        	}
        	
        	//draw radar ship icon
        	drawRadarIcon();
        	break;	//end radar
        case 2:		//book
        	//background
        	Minecraft.getMinecraft().getTextureManager().bindTexture(guiBook);
        	drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        	
        	//if mouse on page button, change button color
        	if(yMouse > guiTop+179 && yMouse < guiTop+193) {
        		if(xMouse > guiLeft+51 && xMouse < guiLeft+73) {
        			drawTexturedModalRect(guiLeft+53, guiTop+182, 0, 192, 18, 10);
        		}
        		else if(xMouse > guiLeft+173 && xMouse < guiLeft+195) {
        			drawTexturedModalRect(guiLeft+175, guiTop+182, 0, 202, 18, 10);
        		}
        	}
        	
        	break;  //end book
        case 3:		//team
        	//background
        	Minecraft.getMinecraft().getTextureManager().bindTexture(guiTeam);
        	drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        	break;	//end team
        case 4:		//target
        	//background
        	Minecraft.getMinecraft().getTextureManager().bindTexture(guiTarget);
        	drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        	
        	//draw ship clicked circle
        	if(this.listClicked[2] > -1 && this.listClicked[2] < 13) {
        		int cirY = 25 + this.listClicked[2] * 12;
            	drawTexturedModalRect(guiLeft+142, guiTop+cirY, 68, 192, 108, 31);
        	}
        	
        	//draw target model
        	drawTargetModel();
        	break;	//end target
        }
	}
	
	//mouse input
	@Override
	public void handleMouseInput() {
		super.handleMouseInput();
		
		int wheel = Mouse.getEventDWheel();
		if(wheel < 0) {			//scroll down
			handleWheelMove(false);
		}
		else if(wheel > 0) {	//scroll up
			handleWheelMove(true);
		}
	}
	
	//handle mouse wheel move
	private void handleWheelMove(boolean isWheelUp) {
		//get list size
		int listSize = 0;
		int listID = -1;
		
		switch(this.guiFunc) {
		case 1:  //radar
			listSize = this.shipList.size();
			listID = 0;
			break;
		case 3:  //team
			listSize = this.teamList.size();
			listID = 1;
			break;
		case 4:  //target
			listSize = this.tarList.size();
			listID = 2;
			break;
		}
		if(listID < 0) return;
		
		if(isWheelUp) {
			listClicked[listID]++;
			listNum[listID]--;
			
			if(listNum[listID] < 0) {
				listNum[listID] = 0;
				listClicked[listID]--;  //���L�Y, �ɦ^1
			}
		}
		else {
			if(this.shipList.size() > 0) {
				listClicked[listID]--;
				listNum[listID]++;
				
				if(listNum[listID] > listSize - 1) {
					listNum[listID] = listSize - 1;
					listClicked[listID]++;
				}
				if(listNum[listID] < 0) {
					listNum[listID] = 0;
					listClicked[listID]++;  //���L�Y, �ɦ^1
				}
			}
		}
	}
	
	//handle mouse click, @parm posX, posY, mouseKey (0:left 1:right 2:middle 3:...etc)
	@Override
	protected void mouseClicked(int posX, int posY, int mouseKey) {
        super.mouseClicked(posX, posY, mouseKey);
        LogHelper.info("DEBUG : click mouse "+mouseKey);
        //get click position
        xClick = posX - this.guiLeft;
        yClick = posY - this.guiTop;
        
        //match all pages
        int getFunc = GuiHelper.getButton(ID.G.ADMIRALDESK, 0, xClick, yClick);
        setDeskFunction(getFunc);
        
        //match radar page
        switch(this.guiFunc) {
        case 1:     //radar
        	int radarBtn = GuiHelper.getButton(ID.G.ADMIRALDESK, 1, xClick, yClick);
        	switch(radarBtn) {
            case 0:	//radar scale
            	this.radar_zoomLv++;
            	if(this.radar_zoomLv > 2) this.radar_zoomLv = 0;
            	break;
            case 1: //ship slot 0~4
            case 2:
            case 3:
            case 4:
            case 5:
            	this.listClicked[0] = radarBtn - 1;
            	break;
            case 6: //open ship GUI
            	this.openShipGUI();
            	break;
            }
        	break;  //end radar
        case 2:     //book
        	int getbtn = GuiHelper.getButton(ID.G.ADMIRALDESK, 2, xClick, yClick);
        	switch(getbtn) {
            case 0:	//left
            	this.book_pageNum--;
            	if(this.book_pageNum < 0) this.book_pageNum = 0;
            	LogHelper.info("DEBUG : desk: book page: "+book_pageNum);
            	break;
            case 1: //right
            	this.book_pageNum++;
            	if(this.book_pageNum > GuiBook.getMaxPageNumber(book_chapNum)) {
            		this.book_pageNum = GuiBook.getMaxPageNumber(book_chapNum);
            	}
            	LogHelper.info("DEBUG : desk: book page: "+book_pageNum);
            	break;
            case 2: //chap 0
            case 3: //chap 1
            case 4: //chap 2
            case 5: //chap 3
            case 6: //chap 4
            case 7: //chap 5
            case 8: //chap 6
            	this.book_chapNum = getbtn - 2;
            	this.book_pageNum = 0;
            	LogHelper.info("DEBUG : desk: book chap: "+book_chapNum);
            	break;
            }
        	break;  //end book
        case 3:		//team
        	break;	//end team
        case 4:     //target
        	int targetBtn = GuiHelper.getButton(ID.G.ADMIRALDESK, 4, xClick, yClick);
        	switch(targetBtn) {
            case 0:	//remove target
            	this.tarList = this.extProps.getTargetClassList();
            	int clicked = this.listNum[2]+this.listClicked[2];
            	
            	if(clicked < this.tarList.size()) {
            		String tarstr = this.tarList.get(clicked);
                	LogHelper.info("DEBUG : desk: remove target class: "+tarstr);
                	//remove clicked target
    				CommonProxy.channelG.sendToServer(new C2SGUIPackets(player, C2SGUIPackets.PID.SetTarClass, tarstr));
            	}
            	break;
            case 1: //target slot
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            	this.listClicked[2] = targetBtn - 1;
            	getEntityByClick();
            	break;
            }
        	break;  //end target
        }
        
        syncTileEntityC2S();
	}
	
//	@Override
//	protected void mouseClickMove(int mx, int my, int button, long time) {
//		super.mouseClickMove(mx, my, button, time);
//	}
	
	private void setDeskFunction(int button) {
		if(button >= 0) {
			if(this.guiFunc != button+1) {
	    		this.guiFunc = button+1;
	    		LogHelper.info("DEBUG : GUI click: desk: click function button");
	    	}
	    	else {
	    		this.guiFunc = 0;
	    	}
			
			syncTileEntityC2S();
		}
	}
	
	//client to server sync
	private void syncTileEntityC2S() {
		this.tile.guiFunc = this.guiFunc;
		this.tile.radar_zoomLv = this.radar_zoomLv;
		this.tile.book_chap = this.book_chapNum;
		this.tile.book_page = this.book_pageNum;
		this.tile.sendSyncPacketC2S();
	}
	
	//draw light spot in radar screen
	private void drawRadarIcon() {
		if(extProps != null) {
			List<Integer> ships = extProps.getShipEIDList();
			
			//icon setting
	    	GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			
			Entity ship;
			double ox = this.tile.xCoord;
			double oy = this.tile.yCoord;
			double oz = this.tile.zCoord;
			int dx = 0;
			int dy = 0;
			int dz = 0;
			int id = 0;
			this.shipList = new ArrayList();
			
			//for all ships in ship list
			for(int eid : ships) {
				ShipEntity getent = new ShipEntity();
				double px = -1;
				double py = -1;
				double pz = -1;
				
				//get ship position
				if(eid > 0) {
					ship = EntityHelper.getEntityByID(eid, 0, true);
					if(ship != null) {
						getent.setShip(ship);
						px = ship.posX;
						py = ship.posY;
						pz = ship.posZ;
					}
				}
				
				//draw ship icon on the radar
				if(py > 0) {
					//change ship position to radar position
					//zoom lv 0: 128x128: 1 pixel = 1 block
					//zoom lv 1: 64x64:   2 pixel = 1 block
					//zoom lv 2: 32x32:   4 pixel = 1 block
					px -= ox;
					py -= oy;
					pz -= oz;
					
					//get scale factor
					float radarScale = 1F;	//256x256: scale = 0.5 pixel = 1 block
					switch(this.radar_zoomLv) {
					case 1:	//64x64
						radarScale = 2;
						break;
					case 2:	//16x16
						radarScale = 4;
						break;
					}
					
					//scale distance
					px *= radarScale;
					py *= radarScale;
					pz *= radarScale;
					
					//radar display distance limit = 64 pixels
					if((int)px > 64) px = 64;
					if((int)px < -64) px = -64;
					if((int)pz > 64) pz = 64;
					if((int)pz < -64) pz = -64;
					
					//add ship to shiplist
					getent.pixelx = guiLeft+69+px;
					getent.pixely = py;
					getent.pixelz = guiTop+88+pz;
					this.shipList.add(getent);
					
					//select icon
					int sIcon = 0;
					if(MathHelper.abs_int((int)px) > 48 || MathHelper.abs_int((int)pz) > 48) {
						sIcon = (int)(this.tickGUI * 0.125F + 6) % 8 * 3;
					}
					else if(MathHelper.abs_int((int)px) > 32 || MathHelper.abs_int((int)pz) > 32) {
						sIcon = (int)(this.tickGUI * 0.125F + 4) % 8 * 3;
					}
					else if(MathHelper.abs_int((int)px) > 16 || MathHelper.abs_int((int)pz) > 16) {
						sIcon = (int)(this.tickGUI * 0.125F + 2) % 8 * 3;
					}
					else {
						sIcon = (int)(this.tickGUI * 0.125F) % 8 * 3;
					}
					
					//draw icon by radar zoom level, radar center = [70,89]
					if(id == this.listNum[0] + this.listClicked[0]) {
						drawTexturedModalRect(guiLeft+69+(int)px, guiTop+88+(int)pz, 0, 195, 3, 3);
					}
					else {
						drawTexturedModalRect(guiLeft+69+(int)px, guiTop+88+(int)pz, sIcon, 192, 3, 3);
					}
					id++;
				}//end y position > 0
			}//end for all ship
			GL11.glDisable(GL11.GL_BLEND);
		}//end get player
	}//end draw radar icon
	
	//draw ship text in radar screen
	private void drawRadarText() {
		String str;
		ShipEntity s;
		int texty = 31;
		
		//draw button text
		str = I18n.format("gui.shincolle:radar.gui");
		int strlen = (int) (this.fontRendererObj.getStringWidth(str) * 0.5F);
		fontRendererObj.drawStringWithShadow(str, 32-strlen, 174, GuiHelper.pickColor(GuiHelper.pickColorName.YELLOW.ordinal()));
		
		//draw ship list in radar
		for(int i = this.listNum[0]; i < shipList.size() && i < this.listNum[0] + 5; ++i) {
			//get ship position
			s = shipList.get(i);
			if(s != null && s.ship != null) {
				//draw name
				fontRendererObj.drawString(s.name, 146, texty, GuiHelper.pickColor(GuiHelper.pickColorName.WHITE.ordinal()));
				texty += 12;
				//draw pos
				str = I18n.format("gui.shincolle:radar.position") + " " + 
					  MathHelper.ceiling_double_int(s.ship.posX) + " , " + 
					  MathHelper.ceiling_double_int(s.ship.posZ) + "  " +
					  I18n.format("gui.shincolle:radar.height") + " " +
					  (int)(s.ship.posY);
				fontRendererObj.drawString(str, 146, texty, GuiHelper.pickColor(GuiHelper.pickColorName.YELLOW.ordinal()));
				texty += 20;
			}
		}
	}
	
	//draw team text
	private void drawTeamText() {
		//draw button text
		String str = I18n.format("gui.shincolle:team.join");
		int strlen = (int) (this.fontRendererObj.getStringWidth(str) * 0.5F);
		fontRendererObj.drawString(str, 31-strlen, 160, GuiHelper.pickColor(GuiHelper.pickColorName.YELLOW.ordinal()));
		
		str = I18n.format("gui.shincolle:team.leave");
		strlen = (int) (this.fontRendererObj.getStringWidth(str) * 0.5F);
		fontRendererObj.drawString(str, 31-strlen, 174, GuiHelper.pickColor(GuiHelper.pickColorName.WHITE.ordinal()));
		
		str = I18n.format("gui.shincolle:team.create");
		strlen = (int) (this.fontRendererObj.getStringWidth(str) * 0.5F);
		fontRendererObj.drawString(str, 82-strlen, 160, GuiHelper.pickColor(GuiHelper.pickColorName.WHITE.ordinal()));
		
		str = I18n.format("gui.shincolle:team.rename");
		strlen = (int) (this.fontRendererObj.getStringWidth(str) * 0.5F);
		fontRendererObj.drawString(str, 82-strlen, 174, GuiHelper.pickColor(GuiHelper.pickColorName.WHITE.ordinal()));
		
//		//draw team list
//		this.teamList = this.extProps.getTargetClassList();
//		int texty = 28;
//		
//		for(int i = this.listNum[2]; i < tarList.size() && i < this.listNum[2] + 13; ++i) {
//			//get ship position
//			str = tarList.get(i);
//			if(str != null) {
//				//draw name
//				fontRendererObj.drawString(str, 146, texty, GuiHelper.pickColor(GuiHelper.pickColorName.WHITE.ordinal()));
//				texty += 12;
//			}
//		}
	}
	
	//get clicked entity by entity simple name
	private void getEntityByClick() {
		String tarStr = null;
		int clicked = this.listClicked[2] + this.listNum[2];
		
		//get target simple name
		if(clicked < this.tarList.size()) {
			tarStr = this.tarList.get(clicked);
		}
		
		if(tarStr != null) {
			Iterator iter = EntityList.classToStringMapping.entrySet().iterator();
			while(iter.hasNext()) {
				Map.Entry getc = (Entry) iter.next();
				Class key = (Class) getc.getKey();
				String val = (String) getc.getValue();
//				LogHelper.info("DEBUG: desk: clicked: "+key.getSimpleName()+"   "+val);
				
				if(tarStr.equals(key.getSimpleName())) {
					this.targetEntity = EntityList.createEntityByName(val, this.player.worldObj);
					break;
				}
			}
		}
	}
	
	//draw target model
	private void drawTargetModel() {
		if(this.targetEntity != null) {
			int x = this.guiLeft + 72;
			int y = this.guiTop + 136;
			float scale = 40F;
			
			//set basic position and rotation
			GL11.glEnable(GL11.GL_COLOR_MATERIAL);
			GL11.glPushMatrix();
			GL11.glTranslatef(x, y, 50.0F);
			GL11.glScalef(-scale, scale, scale);
			GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
			
			//set the light of model (face to player)
			GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
			RenderHelper.enableStandardItemLighting();
			GL11.glRotatef(-135.0F, 0.0F, 1.0F, 0.0F);
			
			//set head look angle
//			GL11.glRotatef(-((float) Math.atan(-120F / 40.0F)) * 20.0F, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(-30F, 0.0F, 1.0F, 0.0F);
//			this.targetEntity.rotationYaw = (float) Math.atan(2110F / 40.0F) * 40.0F;
//			this.targetEntity.rotationPitch = -((float) Math.atan(90F / 40.0F)) * 20.0F;
			GL11.glTranslatef(0.0F, this.targetEntity.yOffset, 0.0F);
			RenderManager.instance.playerViewY = 180.0F;
			RenderManager.instance.renderEntityWithPosYaw(this.targetEntity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
			GL11.glPopMatrix();
			RenderHelper.disableStandardItemLighting();
			GL11.glDisable(GL12.GL_RESCALE_NORMAL);
			OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
		}
	}//end target model
	
	//draw target class text in target screen
	private void drawTargetText() {
		//draw button text
		String str = I18n.format("gui.shincolle:target.remove");
		int strlen = (int) (this.fontRendererObj.getStringWidth(str) * 0.5F);
		fontRendererObj.drawString(str, 31-strlen, 160, GuiHelper.pickColor(GuiHelper.pickColorName.WHITE.ordinal()));
		
		//draw target list
		this.tarList = this.extProps.getTargetClassList();
		int texty = 28;
//		LogHelper.info("DEBUG : gui desk: get list "+tarList.size());
		for(int i = this.listNum[2]; i < tarList.size() && i < this.listNum[2] + 13; ++i) {
			//get ship position
			str = tarList.get(i);
			if(str != null) {
				//draw name
				fontRendererObj.drawString(str, 146, texty, GuiHelper.pickColor(GuiHelper.pickColorName.WHITE.ordinal()));
				texty += 12;
			}
		}
	}
	
	//open ship GUI
	private void openShipGUI() {
		int clickid = this.listNum[0] + this.listClicked[0];
		
		if(clickid > -1 && clickid <= this.shipList.size()) {
			Entity ent = this.shipList.get(clickid).ship;
			LogHelper.info("DEBUG : guiiii  "+clickid);
//			if(ent instanceof BasicEntityShip && EntityHelper.checkSameOwner(player, ent)) {
			if(ent instanceof BasicEntityShip) {
				LogHelper.info("DEBUG : guiiii  "+ent);
				this.mc.thePlayer.closeScreen();
				//send GUI packet
				CommonProxy.channelG.sendToServer(new C2SGUIPackets(player, C2SGUIPackets.PID.OpenShipGUI, ent.getEntityId()));
			}
		}
	}


}

