/*
springs-mass demo using the numerical recipes differential equation solver
yigal irani Wednesday, May 16, 2007 16:12:00 yigal@symbolclick.com
Java porting and some modifications: yigal irani, Friday, August 29, 2008 14:19:46
License: GPL
Friday, June 29, 2012 19:15:54: published to github. see demo at http://symbolclick.com/springmass/
*/
import java.awt.*;
import java.applet.*;
import java.util.*;
import java.text.*;


class Spring{
    int start;
    int end;
};

class Vec{
    double x=0,y=0;
    Vec(double _x,double _y){
    	x=_x;
    	y=_y;
    }
    Vec(){
		x=0;
		y=0;
    }
    double trim(double x,double min_value,double max_value){
		return Math.min(Math.max(x,min_value),max_value);
	}
	Vec trim(double min_value,double max_value){
		return new Vec(
		x=trim(x,min_value,max_value),
		y=trim(y,min_value,max_value)
			);
	}
	Vec div(double a){
		Vec ans = new Vec();
		ans.x=x/a;
		ans.y=y/a;
		return ans;
    }
    Vec add(Vec right){
		Vec ans=new Vec();
		ans.x=x+right.x;
		ans.y=y+right.y;
		return ans;
    }
    void add_to(Vec right){
		x+=right.x;
		y+=right.y;
    }
    void sub_to(Vec right){
		x-=right.x;
		y-=right.y;
    }
    Vec sub(Vec right){
		Vec ans=new Vec();
		ans.x=x-right.x;
		ans.y=y-right.y;
		return ans;
    }
    Vec mult(double scalar){
		Vec ans=new Vec();
		ans.x=x*scalar;
		ans.y=y*scalar;
		return ans;
    }
    double dot_mult(Vec right){
		return x*right.x+y*right.y;
    }
  	double calc_dist(Vec p2){
  		Vec p1=this;
	    return Math.sqrt((p2.x-p1.x)*(p2.x-p1.x)+(p2.y-p1.y)*(p2.y-p1.y));
	}
};

class Ball{
    public Vec pos=new Vec(),speed=new Vec();
    Ball(){

    }
};
class BallVector extends Vector{
	public Ball get2(int i){
		return (Ball)super.get(i);
	}
};

class SpringVector extends Vector{
	public Spring get2(int i){
		return (Spring)super.get(i);
	}
};
class Timer{
	public double cur_time;
	public double time_diff;
	private double epoch_time;
	private double system_time(){
		return System.currentTimeMillis()/1000.;
	}
	Timer(){
		epoch_time=system_time();
	}
	void tell_time(){
		double time=system_time()-epoch_time;
		time_diff=Math.min(time-cur_time,.05);
		cur_time=time;
	}
};

class WorldAnimate{//just one public method
	BallVector balls;
	SpringVector springs;
	double radius;
	static final  double STRING_LEN= .4;
	static final  double NUM_STEPS =10;

	int num_balls;


	double wall_power2(double pos){
	    //todo: use speed to calc friction
	    if (pos+radius>1){
			is_colide=true;
			return -(pos+radius-1)*10000;
	    }
	    if (pos-radius<-1){
			is_colide=true;
	        return -(pos-radius+1)*10000;
	    }
	    return 0;
	}
	boolean is_colide=false;

	Vec wall_power(Ball p){
	    Vec ans=new Vec();
	    is_colide=false;
	    ans.x=wall_power2(p.pos.x);
	    ans.y=wall_power2(p.pos.y);
	    if (is_colide)
	    	ans.sub_to(p.speed.mult(10));
	    return ans;
	}


	Vec calc_collide_power(Ball p1,Ball p2,double dist){
		if (dist>radius*2)
			return new Vec();//Friday, August 29, 2008 15:26:33: stickiness bug fix
	    Vec speed_diff=p2.speed.sub(p1.speed);
	    double force=1000*(dist-radius*2);
	    Vec npos1=p1.pos.div(dist); //normalized
	    Vec npos2=p2.pos.div(dist);
	    force+=10*speed_diff.dot_mult(npos2.sub(npos1));
	    Vec ans=npos2.sub(npos1).mult(force);
	    return ans;
	/*    colide_power_x=force*(x2-x1)/dist;
	    colide_power_y=force*(y2-y1)/dist;*/

	}
	Vec calc_spring_power(Ball p1,Ball p2){
	    double dist=p1.pos.calc_dist(p2.pos);
	    //if (abs(dist-STRING_LEN)<.1)
	//	return;
	    Vec speed_diff=p2.speed.sub(p1.speed);
	    double force=1000*(dist-STRING_LEN);
	    Vec npos1=p1.pos.div(dist); //normalized
	    Vec npos2=p2.pos.div(dist);
	    force+=100*speed_diff.dot_mult(npos2.sub(npos1));
	    Vec ans=npos2.sub(npos1).mult(force);
	    return ans;
	    //force*=force;
	//    colide_power_x=(x2-x1)/dist*force;
	 //   colide_power_y=(y2-y1)/dist*force;

	}

	void encode_balls(BallVector balls,double y[]){

	    for (int i=0;i<num_balls;i++){
	    	Ball p=balls.get2(i);
	        y[i*4+1]=p.pos.x;
	        y[i*4+2]=p.pos.y;
	        y[i*4+3]=p.speed.x;
	        y[i*4+4]=p.speed.y;
	    }
	}
	BallVector decode_balls(double y[]){
		BallVector ans=new BallVector();
	    for (int i=0;i<num_balls;i++){
	        Ball p=new Ball();
	        p.pos.x=y[i*4+1];
	        p.pos.y=y[i*4+2];
	        p.speed.x=y[i*4+3];
	        p.speed.y=y[i*4+4];
			ans.addElement(p);
	    }
	    return ans;

	}
	boolean far_away_fast_calc(double p1,double p2,double dist){
		return (p2-p1>dist||p1-p2>dist);
	}
	boolean far_away_fast_calc(Vec p1,Vec p2,double dist){
		if (far_away_fast_calc(p1.x,p2.x,dist))
			return true;
		if (far_away_fast_calc(p1.y,p2.y,dist))
			return true;
		return false;
	}
	void the_derive(double time,double y[],double dy[]){
	    int i;
	    BallVector balls = decode_balls(y);//new BallVector();//Ball[num_balls];
	    BallVector dballs= new BallVector();//new Ball[num_balls];

	    for (i=0;i<num_balls;i++){
	    	Ball p=balls.get2(i);
	    	Ball d=new Ball();
			d.pos=p.speed;
			d.speed=wall_power(p);
			d.speed.y-=1; //gravity
			dballs.add(d);
	    }

	    for (i=0;i<num_balls;i++)
	    	for (int j=i+1;j<num_balls;j++){
	    		Ball p1=balls.get2(i);
	    		Ball p2=balls.get2(j);
	    		if (far_away_fast_calc(p1.pos,p2.pos,radius*2))
	    			continue;
	    		double dist=p1.pos.calc_dist(p2.pos);
				//if (dist>radius*2)
				//	continue;

				Vec collide_power=calc_collide_power(p1,p2,dist);
			    dballs.get2(i).speed.add_to(collide_power);
			    dballs.get2(j).speed.sub_to(collide_power);
		}
	    for (i=0;i<springs.size();i++){
			Spring s=springs.get2(i);
			Vec collide_power=calc_spring_power(balls.get2(s.start),balls.get2(s.end));
			dballs.get2(s.start).speed.add_to(collide_power);
			dballs.get2(s.end).speed.sub_to(collide_power);
	    }
	    encode_balls(dballs,dy);

	};
	double []new_vector(int size){
		return new double[size+1];
	}
	void call_rk4(double cur_time,double time_diff){
		num_balls=balls.size();
	    double[] y  = new_vector(num_balls*4);// double[num_balls*4];
	    double[] dy = new_vector(num_balls*4);
	    encode_balls(balls,y);
	    the_derive(cur_time,y,dy); //the current implementation of derive does not uses the time, but can envision an implementation that might (gravity is off every second, perhaps?)
	    rk4(y, dy, num_balls*4, cur_time, time_diff, y);
	    //balls=new BallVector();
	    balls=decode_balls(y);
	    //free_vector(y,1,num_balls*4);
//	    free_vector(dy,1,num_balls*4);
	}
	public BallVector calc_new_frame(BallVector _balls, SpringVector _springs,double _radius,Timer timer){ //return the the balls of the next frame
		balls=_balls;
		springs=_springs;
		radius=_radius;
		num_balls=balls.size();
		int i;
	    for (i=0;i<NUM_STEPS;i++)
			call_rk4(timer.cur_time,timer.time_diff/NUM_STEPS);	//too: acum the time?
		return balls;
	}
	void rk4(double y[], double dydx[], int n, double x, double h, double yout[])
	/*translated to java from numerical recipies (see nr.com). here is the original doc:
	 Given values for the variables y[1..n] and their derivatives dydx[1..n] known at x, use the
	fourth-order Runge-Kutta method to advance the solution over an interval h and return the
	incremented variables as yout[1..n], which need not be a distinct array from y. The user
	supplies the routine derivs(x,y,dydx), which returns derivatives dydx at x.*/
	{
		int i;
		double xh,hh,h6;
		double []dym=new_vector(n);
		double []dyt=new_vector(n);
		//16.1 Runge-Kutta Method 713
		double []yt=new_vector(n);
		hh=h*0.5;
		h6=h/6.0;
		xh=x+hh;
		for (i=1;i<=n;i++) yt[i]=y[i]+hh*dydx[i]; //First step.
			the_derive(xh,yt,dyt); //Second step.
		for (i=1;i<=n;i++) yt[i]=y[i]+hh*dyt[i];
			the_derive(xh,yt,dym); //Third step.
		for (i=1;i<=n;i++) {
			yt[i]=y[i]+h*dym[i];
			dym[i] += dyt[i];
		}
		the_derive(x+h,yt,dyt); //Fourth step.
		for (i=1;i<=n;i++) //Accumulate increments with proper
			yout[i]=y[i]+h6*(dydx[i]+dyt[i]+2.0*dym[i]); //weights.
	}
};
class DebugPrinter{
	Graphics g;
	int x;
	int y;
	DecimalFormat df = new DecimalFormat("0.00");

	DebugPrinter(Graphics _g,int _x,int _y){
		g=_g;
		x=_x;
		y=_y;
	}
	void print(String name,int value){
		g.drawString(name+":"+value,x,y+=20);
	}
	void print(String name,double value){
		g.drawString(name+":"+df.format(value),x,y+=20);
	}
	void print(String name,Vec value){
		g.drawString(name+":"+df.format(value.x)+" , "+df.format(value.y),x,y+=20);
	}
	void print(String str){
		g.drawString(str,x,y+=20);
	}
};
public class javaspringdemo extends Applet {
	//Vector balls=new Vector();
	int numpaint=0;
	private Image bufferImage=null;
    Graphics buffer_graphics=null;
	static final  double RADIUS=.05;
	static final  double NUM_POINTS =10;
	static final  double NUM_STRINGS =6;
	int dragged_ball=-1;
	Timer timer=new Timer();
    Dimension dim;
	int size;
	Vec dragged_vec,last_dragged_vec,dragged_speed,find_offset;
	BallVector balls=new BallVector();
	SpringVector springs= new SpringVector();
   	class paint_thread extends Thread{
      public void run(){
        while(true){
          try{
            repaint();
            Thread.sleep(1);
          }catch(Throwable e){
          }
        }
      }
    }
	void init_rand(Ball ball){
	    ball.pos.x=my_rand(-1+2*RADIUS,1-2*RADIUS);
	    ball.pos.y=my_rand(-1+2*RADIUS,1-2*RADIUS);
	    ball.speed.x=my_rand(-1,1);
	    ball.speed.y=my_rand(1,2);
	}
	double my_rand(double min,double max){
	    double r=Math.random();
	    //r=r%1000;
	    return r*(max-min)+min;
	};
	void add_spring(int start,int end){
		Spring s=new Spring();
		s.start=start;
		s.end=end;
		springs.add(s);
	}
	void init_world(){
	    springs = new SpringVector();//[num_springs];
	    add_spring(0,1);
	    add_spring(1,2);
	    add_spring(2,0);
	    add_spring(3,4);
	    add_spring(4,5);
	    add_spring(5,3);
	    add_spring(0,4);
	    balls = new BallVector();//[num_balls];
	    for (int i=0;i<NUM_POINTS;i++){
	    	Ball p= new Ball();
	    	init_rand(p);
	    	balls.addElement(p);
	    }
	}
	void animate(){
		dim = getSize();
		size=(int)(Math.min(dim.height,dim.width)/2.2);
		timer.tell_time();
		if(timer.time_diff==0)
			return;//not enought time has passed, dont animate-crach fix
	    dragged_speed=dragged_vec.sub(last_dragged_vec).div(timer.time_diff);
	    last_dragged_vec=dragged_vec;
		if (dragged_ball!=-1){
			balls.get2(dragged_ball).pos=dragged_vec.add(find_offset).trim(-1,1);
			balls.get2(dragged_ball).speed=dragged_speed;
	    }

		balls=new WorldAnimate().calc_new_frame(balls,springs,RADIUS,timer);
	}


	int find_ball(Vec v){
		int num_balls=balls.size();
	    for (int i=0;i<num_balls;i++){
			Ball p=balls.get2(i);
		double dist=v.calc_dist(p.pos);
		if (dist<RADIUS){
//		    printf("found ball %d\n",i);
			find_offset=p.pos.sub(dragged_vec);
		    //last_dragged_vec=v;
		    return i;
		}
	    }
	    return -1;
	}

	Vec vec_by_mouse(int x,int y){
	    Vec ans=new Vec();
	    ans.x=(x-dim.width/2.)/size;
	    ans.y=-(y-dim.height/2.)/size;

	    return ans;
	}
	int screen_dist(double world_dist){
		return (int)(size*world_dist);
	}
	Point point_by_vec(Vec vec){
		double x=dim.width/2.+screen_dist(vec.x);
		double y=dim.height/2.-screen_dist(vec.y);
		return  new Point((int)x,(int)y);
	}
  	public boolean mouseUp(Event evt,int x,int y){
	    dragged_ball=-1;
		return true;
  	}
	void new_ball(Vec vec){
		Ball b= new Ball();
		b.pos=vec;
		balls.add(b);
	}
	public boolean mouseDown(
                          Event evt,
                          int x,
                          int y){

		Vec vec=vec_by_mouse(x,y);
		dragged_ball=find_ball(vec); //todo: get screen sizze
		//balls.addElement(new Ball(x,y))   ;       // todo: add balls by pressding 'a'
		if (dragged_ball==-1)
			new_ball(vec);
		//repaint(0);
		return true;
	}
	public boolean mouseDrag(Event evt,int x,int y){
	    dragged_vec=vec_by_mouse(x,y);
	    return true;
	}
	public boolean mouseMove(Event evt,int x,int y){
	    dragged_vec=vec_by_mouse(x,y);
	    return true;
	}
	public void init() {
		new paint_thread().start();
		init_world();
		dragged_vec=new Vec();
		last_dragged_vec=new Vec();
	}
	paint_thread da_thread=null;
  	public void paint(Graphics g){
  		draw(getBufferGraphics());
		g.drawImage(bufferImage, 0, 0,null);
    }
    protected Graphics getBufferGraphics(){
        if (bufferImage == null){
          bufferImage= createImage(getSize().width,getSize().height);
          buffer_graphics = bufferImage.getGraphics();
        }
        return buffer_graphics;
    }
	public void update(Graphics g){
        paint(g);
    }

	public void draw(Graphics g) {
		animate();

		g.setColor(Color.black);
		g.fillRect(0,0,dim.width,dim.height);
		g.setColor(Color.white);

		numpaint++;
		DebugPrinter dbg= new DebugPrinter(g,50,60);
		dbg.print("Spring-mass demo by yigal irani, drag balls or create new ones by clicking inside box");
		dbg.print("frame",numpaint);
		dbg.print("fps",1/timer.time_diff);

		Point top_left=point_by_vec(new Vec(-1,1));
		g.draw3DRect(top_left.x,top_left.y,screen_dist(2),screen_dist(2),true);
		for(int i=0;i<springs.size();i++){
			Spring spring=springs.get2(i);
			Point p1=point_by_vec(balls.get2(spring.start).pos);
			Point p2=point_by_vec(balls.get2(spring.end).pos);
			g.drawLine(p1.x,p1.y,p2.x,p2.y);
		}
		for(int i=0;i<balls.size();i++){
			Ball ball=balls.get2(i);
			Point p=point_by_vec(ball.pos);
			int screen_radius=screen_dist(RADIUS);
			g.setColor(Color.blue);
			g.fillOval(p.x-screen_radius,p.y-screen_radius,screen_radius*2,screen_radius*2);

			g.setColor(Color.white);
			g.drawOval(p.x-screen_radius,p.y-screen_radius,screen_radius*2,screen_radius*2);

			g.drawString(""+i,p.x,p.y);
		}
	}
}