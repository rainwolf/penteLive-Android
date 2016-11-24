package be.submanifold.pentelive;

import java.io.*;
import java.util.*;


public class MarksAIPlayer {

    private int game;
    private int level;
    private int seat;
    private int moveNum;
    private List moves;
    private int size = 19;
    
    private final int bsize = 912;
    private final int tsize = 943;
    
    private final int openingBookSize = 600;
    
    private int cp, tn;
    private int obfl, cob, crot, obsize, extnt;
    private int p[]=new int[7], cc[][]=new int[18][7], 
    sx[]=new int[362], sy[]=new int[362];

    private int dx[] = {-1,0,1,-1,1,0,-1,1};
    private int dy[] = {-1,-1,-1,0,1,1,1,0};
    private int rotx[] = {1,1,1,1,-1,-1,-1,-1};
    private int roty[] = {1,1,-1,-1,-1,-1,1,1};
    private int rotf[] = {0,1,0,1,0,1,0,1};
    private int xoff, yoff, rlct; 
    private int rlst[]=new int[800], rrot[]=new int[800], oscr[]=new int[openingBookSize], nom[]=new int[openingBookSize];
    private int bd[][][]=new int[18][size][size], ciel[][]=new int[7][18]; 
      // 18 levels of 19x19 board
      // each ply of the search is a level
      // 0=empty, 1=player 1 stone, 2=player 2;
      // -1=empty space within 2 spaces of a stone
      // the computer will only consider moving to '-1'.
    private int bmove, bscr;
    
    private int scores[], table[], obk[];

    
    public MarksAIPlayer() {
        obfl = 1;
        
        moves = new ArrayList();
    }
    public boolean useOpeningBook() {
        return obfl > 0;
    }

    public void useOpeningBook(boolean useBook) {
        this.obfl = (useBook?1:0);
    }
    public void setSize(int size) {
        //System.out.println("setsize " + size + ","+this.size);
        //Throwable t = new Throwable();
        //t.printStackTrace();
        if (this.size != size) {
            for (int i = 0 ; i < openingBookSize; i++) {
                for (int j = 0; j < 24; j++) {
                    if (obk[i*24+j] != 0) {
                        int m = obk[i*24+j];
                        obk[i*24+j]=convert(this.size, size, m);
                    }
                }
            }
            for (int i = 0; i < om2.length; i++) {
                //int o = om2[i];
                om2[i] = convert(this.size, size, om2[i]);
                //System.out.println("om2 " + o + ","+om2[i]);
            }

            for (int i = 0; i < om3.length; i++) {
                om3[i] = convert(this.size, size, om3[i]);
            }
        }
        
        this.size = size;
        
    }
    public int convert(int oldSize, int newSize, int m) {     
        int x = m / oldSize;
        int y = m % oldSize;
        x = newSize/2 + (oldSize/2-x);
        y = newSize/2 + (oldSize/2-y);//or whatever
        m = y*newSize+x;
        return m;
    }

    public void setSeat(int seat) {
        this.seat = seat;
        
        if (seat == 1) {
            p[1] = level;
            p[2] = 0;
        }
        else if (seat == 2) {
            p[1] = 0;
            p[2] = level;
        }
    }

    public void setLevel(int level) {
        this.level = level;
        
        if (seat == 1) {
            p[1] = level;
            p[2] = 0;
        }
        else if (seat == 2) {
            p[1] = 0;
            p[2] = level;
        }
    }
    public int getLevel() {
        return level;
    }

    public void setGame(int game) {
        if (game == 1) {
            this.game = 1;
        }
        else if (game == 3) {
            this.game = 2;
        }
    }
    
    public void setOption(String name, String value) {

    }
        
    public void addMove(int move) {

        moves.add(new Integer(move));
        
        sx[moveNum + 1] = move % size;
        sy[moveNum + 1] = move / size;

        tn = ++moveNum;
        dmov();
    }

    /** No easy way to undo moves so just clear everything
     *  and start over adding all but the last move.
     */
    public void undoMove() {

        moves.remove(moves.size() - 1);
        List oldMoves = new ArrayList(moves);
        
        clear();
        
        for (Iterator it = oldMoves.iterator(); it.hasNext();) {
            addMove(((Integer) it.next()).intValue());
        }
    }

    public int getMove() {

        tn = moveNum + 1;
        int move = cmove();
        
        return move;
    }   

    public void init() {
        clear();
    }
    public void init(InputStream scs, InputStream opnbk, InputStream tbl) throws Throwable {
        clear();

        initDataStructures();
        
        loadPenteTBL(tbl);
        loadPenteSCS(scs);
        loadOPNGBK(opnbk);
    }
    
    public void destroy() {   
    }



    private void initDataStructures() {

        scores = new int[bsize*14];
        table = new int[tsize*4];
        obk = new int[openingBookSize * 24];
    }
    
    private void loadPenteSCS(InputStream in) throws Exception {
        
        for(int i=0; i<912; i++) {
            for(int j=0; j<14; j++) {

                int sint=getShort(in);
                scores[i*14+j]=sint;
                //System.out.println(scores[i*14+j]);
            }
        }
        
        //log4j.info("loaded scs");
        //System.out.println("loaded scs");
    }
    
    private void loadPenteTBL(InputStream in) throws Exception {
        
        for(int i=0; i<943; i++) {
            for(int j=0; j<4; j++) {

                int sint=getShort(in);
                table[i*4+j]=sint;
                //System.out.println(table[i*4+j]);
            }
        }

        //log4j.info("loaded tbl");
        //System.out.println("loaded tbl");
    }
    
    private void loadOPNGBK(InputStream in) throws Exception {
        
        obsize=getShort(in);
        if(obsize>=openingBookSize) return;
        cob=0;
        int i=0, sint=0, ef=0;
        
        do {
            i=cob;
            sint=ef=getShort(in);
            nom[i]=sint;
            if(ef!=-1) {
                getShort(in);
                sint=getShort(in);
                oscr[i]=sint;
                //System.out.println(sint);
                for(int j=0; j<nom[i]; j++) {
                    sint=getShort(in);
                    //System.out.println(sint);
                    obk[i*24+j]=sint;
                }
                cob++;
            }
        } while(ef!=-1 && cob<obsize);
        

        //System.out.println("loaded opnbk");
        //log4j.info("loaded gbk");
    }

    public int[] getTbl() {
        return table;
    }
    public int[] getSrc() {
        return scores;
    }
    //need some trickery here to get the same thing the c code
    //does.
    private int getShort(InputStream s) throws Exception {
        
        int i1=s.read(); if(i1==-1) return -1;
        int i2=s.read(); if(i2==-1) return -1;
        
        i2 = i2<<8;
        i2 = i2 | i1;  
        i2 = ((short)i2);
        
        return i2;
    }

    public void clear() {

        for (int i = 0; i < sx.length; i++) {
            sx[i] = 0;
            sy[i] = 0;
        }

        for (int x=0; x<size; x++)   //clear board at beginning
          for (int y=0; y<size; y++)
            bd[0][x][y]=0;
        ciel[1][0]=24;  //set cieling of search to max
        ciel[2][0]=24;
        cc[0][1]=0; //number of captures for p1 (at level 0)
        cc[0][2]=0; // "" p2
        //vct=0;   //no vct search
        obfl=1;  //opening book on
        extnt=2; //extent 2
        tn=0;
        moveNum=0;
        moves.clear();
    }
    
    public void dmov()
    {
        //System.out.println("start dmov() " + tn);
        
        int i, j, k, x, y, cx, cy, obi, mfl, kfl;
        int c1,c2,c3,c4,c5,c6,c7,c8,d;

        cp=2-tn%2;  //set current player
        bd[0][sx[tn]][sy[tn]]=2-tn%2;  //place piece (1 or 2) on board
        for (x=sx[tn]-extnt; x<sx[tn]+1+extnt; x++) //set spaces around piece to -1
          for (y=sy[tn]-extnt; y<sy[tn]+1+extnt; y++) //for consideration by ai
            if (x>=0 && x<size && y>=0 && y<size)
              if (bd[0][x][y]==0) bd[0][x][y]=-1;

          // chk captures
          for (d=0; d<8; d++) {
            c1=sx[tn]+dx[d];
            c2=sy[tn]+dy[d];
            c3=c1+dx[d];
            c4=c2+dy[d];
            c5=c3+dx[d];
            c6=c4+dy[d];
            c7=c5+dx[d];
            c8=c6+dy[d];
            if (c5>=0 && c5<size && c6>=0 && c6<size)
              if (bd[0][c1][c2]>0 && bd[0][c3][c4]>0 &&
                  bd[0][c1][c2]!=cp && bd[0][c3][c4]!=cp) {
                if (bd[0][c5][c6]==cp) {
                  cc[0][cp]+=2;
                  bd[0][c1][c2]=-1;
                  bd[0][c3][c4]=-1;
                }
                else {
                  if (c7>=0 && c7<size && c8>=0 && c8<size && game==2)
                    if (bd[0][c7][c8]==cp && bd[0][c5][c6]>0) {
                      cc[0][cp]+=3;
                      bd[0][c1][c2]=-1;
                      bd[0][c3][c4]=-1;
                      bd[0][c5][c6]=-1;
                    }
                }
              } // if en*2
          }  // next d

        if (tn==1) {
            xoff=yoff=-size/2;
        }
        if (tn>1 && obfl!=0) {
        //if (tn>1 && obfl) {
          rlct=0;
          for (i=0; i<8; i++) {
            for (obi=0; obi<obsize; obi++) {
              mfl=1;
              for (j=1; j<=tn; j++) {
                cx=(sx[j]+xoff)*rotx[i];
                cy=(sy[j]+yoff)*roty[i];
                if (rotf[i]!=0) { c1=cx; cx=cy; cy=c1; }
                //if (rotf[i]) { c1=cx; cx=cy; cy=c1; }
                kfl=0;
                
                if (obk[obi*24+j-1]==((cy+size/2)*size+cx+size/2)) kfl=1;
                //if (*(obk+obi*24+j-1)==(cy+9)*size+cx+9) kfl=1;
                if (j<5) {   // symmetry for moves  1,3 and 2,4
                  k=j+2;
                  if (k>4) k=j-2;
                  if (obk[obi*24+k-1]==((cy+size/2)*size+cx+size/2)) kfl=1;
                  //if (*(obk+obi*24+k-1)==(cy+9)*size+cx+9) kfl=1;
                }
                if (kfl==0) mfl=0;
                //if (!kfl) mfl=0;
              } // next j
              if (p[3-cp]!=0 && mfl!=0 && (cp==2 && oscr[obi]>5 || cp==1 && oscr[obi]<5))
              //if (p[3-cp] && mfl && (cp==2 && oscr[obi]>5 || cp==1 && oscr[obi]<5))
                mfl=0;  //opening book score <5 = p1 adv; >5 = p2 adv
              if (mfl!=0) {
              //if (mfl) {
                rlst[rlct]=obi;
                rrot[rlct]=i;
                rlct++;
                if (rlct>799) rlct=799;
              }
            } // next obi
          } // next i

          if (rlct==0 && tn==4) {  // offset capture
          //if (!rlct && tn==4) {  // offset capture
            xoff=sx[1]-sx[3]-size/2;
            yoff=sy[1]-sy[3]-size/2;
            for (i=0; i<8; i++) {
              for (obi=0; obi<obsize; obi++) {
                mfl=1;
                for (j=1; j<=tn; j++) {
                  cx=(sx[j]+xoff)*rotx[i];
                  cy=(sy[j]+yoff)*roty[i];
                  if (rotf[i]!=0) { c1=cx; cx=cy; cy=c1; }
                  //if (rotf[i]) { c1=cx; cx=cy; cy=c1; }
                  kfl=0;
                  if (obk[obi*24+j-1]==((cy+size/2)*size+cx+size/2)) kfl=1;
                  //if (*(obk+obi*24+j-1)==(cy+9)*size+cx+9) kfl=1;
                  if (j<5) {   // symmetry for moves  1,3 and 2,4
                    k=j+2;
                    if (k>4) k=j-2;
                    if (obk[obi*24+k-1]==((cy+size/2)*size+cx+size/2)) kfl=1;
                    //if (*(obk+obi*24+k-1)==(cy+9)*size+cx+9) kfl=1;
                  }
                  if (kfl==0) mfl=0;
                  //if (!kfl) mfl=0;
                } // next j
                if (p[3-cp]!=0 && mfl!=0 && (cp==2 && oscr[obi]>5 || cp==1 && oscr[obi]<5))
                //if (p[3-cp] && mfl && (cp==2 && oscr[obi]>5 || cp==1 && oscr[obi]<5))
                  mfl=0;  //opening book score <5 = p1 adv; >5 = p2 adv
                if (mfl!=0) {
                //if (mfl) {
                  rlst[rlct]=obi;
                  rrot[rlct]=i;
                  rlct++;
                  if (rlct>799) rlct=799;
                }
              } // next obi
            } // next i
          } // end capture
          if (rlct==0) {
              obfl=0;
              //System.out.println("turn off opening book");
          }
          //if (!rlct) obfl=0;
          else {
            i=(int)(Math.random()*(rlct-1));
            //i=rand()*(rlct-1)/32768;
            cob=rlst[i];
            crot=rrot[i];
          }
        } // if turn>1
        if (tn>4 && tn>=nom[cob]) {
            obfl=0;
            //System.out.println("turn off opening book 2");
        }
        
        //System.out.println("end dmov()");
    }
    
    private final int om2[] = {181,182,162,163,164,165,144,145};
    private final int op2[] = {25,36,77,82,93,95,97,99};
    private final int om3[] = {183,184,202,221,240,260,239,238,237,256,236,235,
        234,252,215,196,177,176,158,139,120,100,
        121,122,123,104,124,125,126,108,145,164};
    
    private int cmove() 
    {
        //System.out.println("start cmov() obfl="+obfl+",tn="+tn+",s="+size);
        
        int i, x, y, xx,t;

          //opening moves for turn 2 and 3.

        cp=2-tn%2;
        bmove=0;
        bscr=0;
        
//        hlim=4;
//        if (game==2) hlim=5;
        
        if (tn==1) bmove=180;
        if (tn==2 && obfl!=0) {
            //System.out.println("1");
        //if (tn==2 && obfl) {
          t=(int)(Math.random()*99);
          //x=rand()*99/32768;
          i=-1;
          do i++;
          while (t>op2[i]);
          x=om2[i]%size-size/2;
          y=om2[i]/size-size/2;
          ////System.out.println(i+","+t+","+x+","+y);
          if (((int)(Math.random()*2)) == 1) x=-x;
          if (((int)(Math.random()*2)) == 1) y=-y;
          if (((int)(Math.random()*2)) == 1) { xx=x; x=y; y=xx; }
          //if (rand()%2) x=-x;
          //if (rand()%2) y=-y;
          //if (rand()%2) { xx=x; x=y; y=xx; } 
          bmove=(y+size/2)*size+x+size/2;
          ////System.out.println(x+","+y+","+bmove);
//        if (bmove == 84) {
//            //System.out.println("84 " + i);
//        }
          i=0;
        }
        else if (tn==2) {
            //System.out.println("2");
          do {
            x=7+((int)(Math.random()*3));
            y=7+((int)(Math.random()*3));
            //x=7+rand()/8192; //0-3
            //y=7+rand()/8192;
          } while (bd[0][x][y]>0);
          bmove=y*size+x;
        }
        if (tn==3 && obfl==0) {
            //System.out.println("3");
        //if (tn==3 && !obfl) {
          do {
            i=((int)(Math.random()*31));
            //i=rand()*31/32768;
            x=om3[i]%size;
            y=om3[i]/size;
          } while (bd[0][x][y]>0);
          bmove=y*size+x;
        }
        if (obfl!=0 && tn>2) {
            //System.out.println("4");
        //if (obfl && tn>2) {
          if ( ((int)(Math.random()*99)) < tn*6-23 ) {
              //System.out.println("5");
              obfl=0;
              //System.out.println("turn off opening book 3");
          }
          //if (rand()*99/32768 < tn*6-23) obfl=0;
          else {
              //System.out.println("6");
            x=obk[cob*24+tn-1]%size-size/2;
            //x=*(obk+cob*24+tn-1)%19-9;
            y=obk[cob*24+tn-1]/size-size/2;
            //y=*(obk+cob*24+tn-1)/19-9;
            if (rotf[crot]!=0) { xx=x; x=y; y=xx; }
            //if (rotf[crot]) { xx=x; x=y; y=xx; }
            x=x*rotx[crot]-xoff;
            y=y*roty[crot]-yoff;
            if (tn==4 && bd[0][x][y]>0) {   // flip moves 2 and 4
              x=obk[cob*24+tn-3]%size-size/2;
              //x=*(obk+cob*24+tn-3)%19-9;
              y=obk[cob*24+tn-3]/size-size/2;
              //y=*(obk+cob*24+tn-3)/19-9;
              if (rotf[crot]!=0) { xx=x; x=y; y=xx; }
              //if (rotf[crot]) { xx=x; x=y; y=xx; }
              x=x*rotx[crot]-xoff;
              y=y*roty[crot]-yoff;
            }
            bmove=y*size+x;
          }
        }

        //System.out.println("bmove="+bmove);
        /*
        if (bmove==0) {
            //System.out.println("no opening book, search");
        //if (!bmove) {
          plv=p[cp];
          tree();
        } // end if hmv=0;
*/
        if (bmove==0) {

            //System.out.println("7");
            bmove=-1;//indicates not opening book
        }
        if (bscr<11000) ciel[cp][0]=24;

        //lvl=0;
        
        //System.out.println("bmove="+bmove);
        
        //System.out.println("end cmov()");
        
        return bmove;
    }


//    public static int getX(int pointer, int array[][]) {
//        return pointer/array[0].length;
//    }
//    public static int getY(int pointer, int array[][]) {
//        return pointer%array[0].length;
//    }
    
    //public static int getX(int pointer, int array[][][]) {
    //  return 0;
    //}
    //public static int getY(int pointer, int array[][][]) {
    //  return 0;
    //}
    //public static int getZ(int pointer, int array[][][]) {
    //  return 0;
    //}
    
   


//    int eval_s0, j, s[]=new int[7];
//    int x9, y9, bl, tfr, tcap1;
    
}