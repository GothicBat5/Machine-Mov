;SKIN (.SKN) - COMPILE WITH FASM

include '../../skin.inc'

SKIN_PARAMS \
  height = bmp_base.height,\  
  margins = [5:4:37:0],\       
  colors active = [binner=0x000000:\   
                     bouter=0x000000:\   
                     bframe=0x2d2d2d],\
  colors inactive = [binner=0x000000:\  
                     bouter=0x000000:\  
                     bframe=0x2d2d2d],\   
  dtp = 'default.dtp'  

SKIN_BUTTONS \
  close    = [-19:3][16:16],\          
  minimize = [-36:3][16:16]              

SKIN_BITMAPS \
  left active = bmp_left,\          
  left inactive = bmp_left1,\
  oper active= bmp_oper,\
  oper inactive = bmp_oper1,\
  base active = bmp_base,\
  base inactive = bmp_base1

BITMAP bmp_left ,'left.bmp'             
BITMAP bmp_oper ,'oper.bmp'
BITMAP bmp_base ,'base.bmp'
BITMAP bmp_left1,'left.bmp'
BITMAP bmp_oper1,'oper_1.bmp'
BITMAP bmp_base1,'base.bmp'
