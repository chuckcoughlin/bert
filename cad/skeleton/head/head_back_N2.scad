// Modify the original Poppy head_back to accomodate
// the Odroid-N2 computer board. The 
board_length = 90;     // Circuit board length
board_width  = 90;     // Circuit board length
board_height = 25;     // Height of populated board
back_offset  = 500;    // Origin to back of head
offset_x=-0;  
offset_y=-320;
offset_z=0;
// Place skull in center, align to axes
// x to sides, y forward and back, z is top and bottom
// Offsets make origin in center of head
module load_stl() {
    color("gray")
    rotate([120,0,0])
    translate([offset_x,offset_y,offset_z])
    import(file="head_back.STL",convexity=5);
}

// Use this box to clear back-side
module back_opening() {
    color("green");
    translate([0,50,15]) {
        cube([board_width,20,board_height],true);
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

difference() {
//union() {
    union() {
        load_stl();
        tie_handle();
    }
    back_opening();
}