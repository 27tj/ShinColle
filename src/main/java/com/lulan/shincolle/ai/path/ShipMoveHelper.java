package com.lulan.shincolle.ai.path;

import com.lulan.shincolle.entity.IShipNavigator;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.util.MathHelper;

/**SHIP MOVE HELPER
 * �t�Xship navigator�ϥ�, �B�~�W�[y�b���ʶq, �A�Ω�����έ���entity
 */
public class ShipMoveHelper {
    /** The EntityLiving that is being moved */
    private EntityLiving entity;
    private IShipNavigator entityN;
    private double posX;
    private double posY;
    private double posZ;
    /** The speed at which the entity should move */
    private double speed;
    private boolean update;


    public ShipMoveHelper(EntityLiving entity) {
        this.entity = entity;
        this.entityN = (IShipNavigator) entity;
        this.posX = entity.posX;
        this.posY = entity.posY;
        this.posZ = entity.posZ;
    }

    public boolean isUpdating() {
        return this.update;
    }

    public double getSpeed() {
        return this.speed;
    }

    /**
     * Sets the speed and location to move to
     */
    public void setMoveTo(double x, double y, double z, double speed) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.speed = speed;
        this.update = true;
    }

    /**CHANGE: �W�[y�b���ʭp��, ���a�۵M�����Ϊ̸��D�Ӳ���y�b
     * �A�Ω�ship��airplane
     */
    public void onUpdateMoveHelper() {
        this.entity.setMoveForward(0.0F);

        if(this.update) {
            this.update = false;
            
            //�p��ؼ��I��ثe�I�t�Z
            int i = MathHelper.floor_double(this.entity.boundingBox.minY + 0.5D);
            double x1 = this.posX - this.entity.posX;
            double z1 = this.posZ - this.entity.posZ;
            double y1 = this.posY - this.entity.posY;
//            double y1 = this.posY - (double)i;
            double moveSq = x1 * x1 + y1 * y1 + z1 * z1;
            
            //�Y���ʭȰ��j, �h�p�⨭�魱�V��V, �H��y�b���ʰʧ@
            if(moveSq >= 0.1D) {
                float f = (float)(Math.atan2(z1, x1) * 180.0D / Math.PI) - 90.0F;
                float moveSpeed = (float)(this.speed * this.entity.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue());
//                LogHelper.info("DEBUG : moveHelper: update f "+(x1 * x1 + z1 * z1));
                this.entity.rotationYaw = this.limitAngle(this.entity.rotationYaw, f, 30.0F);
//                this.entity.setAIMoveSpeed(moveSpeed);

                //y�b����: �ѩ�x��setAIMoveSpeed�u���Ѥ�������, �]��y�b���ʥ����ۦ�]�w
                //y�b�t��, �Ctick�W��Ȭ�: ���ʳt�ת� 20%
                //���ʳt�׭���10%, �קK�������ʤӻ�
                if(entityN.canFly()) {
                	if(y1 > 0.5D) {
                    	this.entity.motionY += moveSpeed * 0.15D;
                    	moveSpeed *= 0.7F;
                    }
                    else if(y1 < -0.5D){
                    	this.entity.motionY -= moveSpeed * 0.15D;
                    	moveSpeed *= 0.7F;
                    }
                }
                else if(EntityHelper.checkEntityIsInLiquid(entity)) {
                	if(y1 > 0.3D) {
                    	this.entity.motionY += moveSpeed * 0.2D;
                    	moveSpeed *= 0.2F;
                    }
                    else if(y1 < -0.3D){
                    	this.entity.motionY -= moveSpeed * 0.2D;
                    	moveSpeed *= 0.2F;
                    }
                	
                	//�Y�b�ؼХ��U��, �h���ո���
                    if(y1 > 0.0D && x1 * x1 + z1 * z1 < 4.0D) {
                        this.entity.getJumpHelper().setJumping();
                    }
                }
                else if(y1 > 0.0D && x1 * x1 + z1 * z1 < 4.0D) {	//�Ω󳰤W���D
                    this.entity.getJumpHelper().setJumping();
                }
//                LogHelper.info("DEBUG : moveHelper: speed "+moveSpeed);
                this.entity.setAIMoveSpeed(moveSpeed);
            }
        }
    }

    /**
     * Limits the given angle to a upper and lower limit.
     */
    private float limitAngle(float yaw, float degree, float limit) {
        float f3 = MathHelper.wrapAngleTo180_float(degree - yaw);

        if(f3 > limit) {
            f3 = limit;
        }

        if(f3 < -limit) {
            f3 = -limit;
        }

        return yaw + f3;
    }
}
