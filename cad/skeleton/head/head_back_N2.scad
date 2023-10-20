// Modify the original Poppy head_back to accomodate
// the Odroid-N2 computer board. The 
board_length = 90;     // Circuit board length
board_width  = 90;     // Circuit board length
back_offset  = 500;    // Origin to back of head
center_offset= 0;      // Origin to bottom of board
// Place skull in center, align to axes
// x to sides, y forward and back, z is top and bottom
module load_stl() {
    rotate([45,0,0])
    translate([0,0,0])
    import(file="head_back.STL",convexity=5);
}

// Use this box to clear back-side
module back_opening() {
}

// Back-side handle for wire tie
module tie_handle() {
    r  = 20;    // Handle radius
    t  = 8;    // Handle thickness
    color("green",1.0) 
    rotate([90,90,90])
     translate([0,0,0]) {
        cylinder(t+1,r,r,true); 
    }
}

union() {
    load_stl();
    tie_handle();
}