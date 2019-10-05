// Import the Poppy face model. Use simple shapes to cut out the actual face,
// leaving only the collar which screws into the back of the head.

// Load the face
module load_stl() {
 import("face_raw.stl");
}

module base() {
    translate([0,0,-18]) {
        cube( size=[200,150,20],center=true);
    }
}

// Use a triangular prism to block out the neck area.
module prism() {
    
 translate([80,-16,16]) {
 rotate([0,90,180]) {
     linear_extrude(height=160) {
    polygon([[25,20],[0,20],[25,0]]);
 }}}
}
module neck() {
    translate([0,-55,4]) {
        cube( size=[160,40,25],center=true);
    }
}
union() {
    //load_stl();
    base();
    prism();
    neck();
}