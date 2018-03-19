// This file was taken from Linux 4.4
// This is a part of the file: sound/pci/au88x0/au88x0_core.c 

/*
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Library General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
 
#ifndef CHIP_AU8810
static void vortex_fifo_clearwtdata(vortex_t * vortex, int fifo, int x)
{
	if (x < 1)
		return;
	for (x--; x >= 0; x--)
		hwwrite(vortex->mmio,
			VORTEX_FIFO_WTDATA +
			(((fifo << FIFO_SIZE_BITS) + x) << 2), 0);
}

static void vortex_fifo_wtinitialize(vortex_t * vortex, int fifo, int j)
{
	vortex_fifo_clearwtdata(vortex, fifo, FIFO_SIZE);
#ifdef CHIP_AU8820
	hwwrite(vortex->mmio, VORTEX_FIFO_WTCTRL + (fifo << 2),
		(FIFO_U1 | ((j & FIFO_MASK) << 0xb)));
#else
	hwwrite(vortex->mmio, VORTEX_FIFO_WTCTRL + (fifo << 2),
		(FIFO_U1 | ((j & FIFO_MASK) << 0xc)));
#endif
}

static void vortex_fifo_setwtvalid(vortex_t * vortex, int fifo, int en)
{
	hwwrite(vortex->mmio, VORTEX_FIFO_WTCTRL + (fifo << 2),
		(hwread(vortex->mmio, VORTEX_FIFO_WTCTRL + (fifo << 2)) &
		 0xffffffef) | ((en & 1) << 4) | FIFO_U1);
}

static void
vortex_fifo_setwtctrl(vortex_t * vortex, int fifo, int ctrl, int priority,
		      int empty, int valid, int f)
{
	int temp = 0, lifeboat = 0;
	int this_4 = 2;

	do {
		temp = hwread(vortex->mmio, VORTEX_FIFO_WTCTRL + (fifo << 2));
		if (lifeboat++ > 0xbb8) {
			dev_err(vortex->card->dev,
				"vortex_fifo_setwtctrl fail\n");
			break;
		}
	}
	while (temp & FIFO_RDONLY);

	if (valid) {
		if ((temp & FIFO_VALID) == 0) {
			vortex_fifo_clearwtdata(vortex, fifo, FIFO_SIZE);	// this_4
#ifdef CHIP_AU8820
			temp = (this_4 & 0x1f) << 0xb;
#else
			temp = (this_4 & 0x3f) << 0xc;
#endif
			temp = (temp & 0xfffffffd) | ((ctrl & 1) << 1);
			temp = (temp & 0xfffffff3) | ((priority & 3) << 2);
			temp = (temp & 0xffffffef) | ((valid & 1) << 4);
			temp |= FIFO_U1;
			temp = (temp & 0xffffffdf) | ((empty & 1) << 5);
#ifdef CHIP_AU8820
			temp = (temp & 0xfffbffff) | ((f & 1) << 0x12);
#endif
#ifdef CHIP_AU8830
			temp = (temp & 0xf7ffffff) | ((f & 1) << 0x1b);
			temp = (temp & 0xefffffff) | ((f & 1) << 0x1c);
#endif
#ifdef CHIP_AU8810
			temp = (temp & 0xfeffffff) | ((f & 1) << 0x18);
			temp = (temp & 0xfdffffff) | ((f & 1) << 0x19);
#endif
		}
	} else {
		if (temp & FIFO_VALID) {
#ifdef CHIP_AU8820
			temp = ((f & 1) << 0x12) | (temp & 0xfffbffef);
#endif
#ifdef CHIP_AU8830
			temp =
			    ((f & 1) << 0x1b) | (temp & 0xe7ffffef) | FIFO_BITS;
#endif
#ifdef CHIP_AU8810
			temp =
			    ((f & 1) << 0x18) | (temp & 0xfcffffef) | FIFO_BITS;
#endif
		} else
			/*if (this_8[fifo]) */
			vortex_fifo_clearwtdata(vortex, fifo, FIFO_SIZE);
	}
	hwwrite(vortex->mmio, VORTEX_FIFO_WTCTRL + (fifo << 2), temp);
	hwread(vortex->mmio, VORTEX_FIFO_WTCTRL + (fifo << 2));

/*	
    do {
		temp = hwread(vortex->mmio, VORTEX_FIFO_WTCTRL + (fifo << 2));
		if (lifeboat++ > 0xbb8) {
			pr_err( "Vortex: vortex_fifo_setwtctrl fail (hanging)\n");
			break;
		}
    } while ((temp & FIFO_RDONLY)&&(temp & FIFO_VALID)&&(temp != 0xFFFFFFFF));
	
	
	if (valid) {
		if (temp & FIFO_VALID) {
			temp = 0x40000;
			//temp |= 0x08000000;
			//temp |= 0x10000000;
			//temp |= 0x04000000;
			//temp |= 0x00400000;
			temp |= 0x1c400000;
			temp &= 0xFFFFFFF3;
			temp &= 0xFFFFFFEF;
			temp |= (valid & 1) << 4;
			hwwrite(vortex->mmio, VORTEX_FIFO_WTCTRL + (fifo << 2), temp);
			return;
		} else {
			vortex_fifo_clearwtdata(vortex, fifo, FIFO_SIZE);
			return;
		}
	} else {
		temp &= 0xffffffef;
		temp |= 0x08000000;
		temp |= 0x10000000;
		temp |= 0x04000000;
		temp |= 0x00400000;
		hwwrite(vortex->mmio, VORTEX_FIFO_WTCTRL + (fifo << 2), temp);
		temp = hwread(vortex->mmio, VORTEX_FIFO_WTCTRL + (fifo << 2));
		//((temp >> 6) & 0x3f) 
		
		priority = 0;
		if (((temp & 0x0fc0) ^ ((temp >> 6) & 0x0fc0)) & 0FFFFFFC0)
			vortex_fifo_clearwtdata(vortex, fifo, FIFO_SIZE);
		valid = 0xfb;
		temp = (temp & 0xfffffffd) | ((ctrl & 1) << 1);
		temp = (temp & 0xfffdffff) | ((f & 1) << 0x11);
		temp = (temp & 0xfffffff3) | ((priority & 3) << 2);
		temp = (temp & 0xffffffef) | ((valid & 1) << 4);
		temp = (temp & 0xffffffdf) | ((empty & 1) << 5);
		hwwrite(vortex->mmio, VORTEX_FIFO_WTCTRL + (fifo << 2), temp);
	}
	
	*/

	/*
	   temp = (temp & 0xfffffffd) | ((ctrl & 1) << 1);
	   temp = (temp & 0xfffdffff) | ((f & 1) << 0x11);
	   temp = (temp & 0xfffffff3) | ((priority & 3) << 2);
	   temp = (temp & 0xffffffef) | ((valid & 1) << 4);
	   temp = (temp & 0xffffffdf) | ((empty & 1) << 5);
	   #ifdef FIFO_BITS
	   temp = temp | FIFO_BITS | 40000;
	   #endif
	   // 0x1c440010, 0x1c400000
	   hwwrite(vortex->mmio, VORTEX_FIFO_WTCTRL + (fifo << 2), temp);
	 */
}

#endif
