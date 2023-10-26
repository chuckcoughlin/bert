// Modify the original Poppy head_back to accomodate
// the Odroid-N2 computer board. 
brd_height = 27; 
brd_length = 90;     // Circuit board length
brd_width  = 90;     // Circuit board length
back_offset  = 500;    // Origin to back of head
offset_x=-0;  
offset_y=-320;
offset_z=0;
// Place skull in center, align to axes
// x to sides, y forward and back, z is top and bottom
// Offsets make origin in center of head
module load_stl() {
    color("gray")
    rotate([125,0,0])
    translate([offset_x,offset_y,offset_z])
    import(file="head_back.STL",convexity=5);
}

// Use this box to clear back-side and a grove along the rail
module back_opening() {
    color("green");
    union() {
    translate([0,50,23]) {   // x,  ,   
        cube([brd_width,25,brd_height],true);
    }
    translate([brd_width/2+1,30,17]) { // right groove
      cube([2,brd_length,20],true);     
    }
    translate([-brd_width/2-1,30,17]) { // left groove
      cube([2,brd_length,20],true);
    }
    translate([brd_width/2+3,30,21]) { // tiny groove right
      cube([2,brd_length,5],true);
    }
    translate([-brd_width/2-3,30,21]) { // tiny groove left
      cube([2,brd_length,5],true);
    }
  }
}

// Rack to hold the printed circuit board
module rack(){
    w  = 10;   // Rail width
    t1 = 6;    // Rail thickness
    t2 = 4;    // Cross piece thickness
    //color("blue")
    union() {
       // Left rail 
       translate([brd_width/2-w/2,22,t1+t2]) {
         cube([w,brd_length,6],true);
       }
       // Right rail 
       translate([-brd_width/2+w/2,22,t1+t2]) {
         cube([w,brd_length,6],true);
       }
       // Back rail 
       translate([0,brd_length/2,t2+1]) {
         cube([brd_width,w,t2],true);
       }
       // Front rail
       color("yellow")
       translate([0,38-brd_length/2-w/2,t2+1]) {  // x,y,z
         cube([brd_width+10,w,t2],true); // Extra width for support
       }
    
  }
  // Screw holes are 71 mm apart, and 72mm front-to back
  union() {
    translate([0,25,0]) {
      screw_hole(-35.5,36); 
      screw_hole(35.5,-36);
      screw_hole(-35.5,-36);
      screw_hole(35.5,36);
    }
  }
}

module screw_hole(x,y) {
    sr = 2.5;          // Radius for screw hole
    sl = 20;           // Length
    translate([x,y,8])
    cylinder(sl,sr,sr,true,$fn=128);
}

// Additional support near servo mount
module support_front(x,y,z) {
    color("green")
    rotate([90,0,270])
     translate([x,y,z-20]) {
    linear_extrude(height=2,center=true) {
        polygon(points=[[0,0],[0,30],[5,30],[12,30]]);
    }
  }
}

// Back-side handle for wire tie
module tie_handle() {
    r  = 8;    // Handle radius
    t  = 3;    // Handle thickness
    color("green",1.0) 
    rotate([90,90,90])
     translate([27,36,0]) {
         difference() {
            cylinder(t+1,r,r,true);
            cylinder(t+2,r-t,r-t,true);
         }
    }
}
union() {
  difference() {
    union() {
        load_stl();
        tie_handle();
    }
    back_opening();
  }
  rack();
  support_front(0,0,20);
  support_front(0,0,-20);
}