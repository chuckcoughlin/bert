// OpenSCAD for the top of a box to hold the power supplies
//          110V 60hz to 12V conversion for Bert
//          Origin = center, bottom of top plate
//          Orientation is upside down

height    = 35;        // Inside height (1/2 high)
length    =240;        // Inside length
thickness =  4;        // Wall and bottom thickness
width     = 70;        // Inside width

// Top plate. 
// x = length
// y = width
module base(x,y) {
    $fn=100;
    linear_extrude(thickness,center=true,convexity=10) {
       square([x,y],center=true); 
    }
}

module end(y,z) {
   linear_extrude(thickness,center=true,convexity=10) {
       square([y,z],center=true); 
    } 
}
module side(x,z) {
   linear_extrude(thickness,center=true,convexity=10) {
       square([x,z],center=true); 
    } 
}
// Half circle opening
module dc_wire_opening(z) {
    wr  = 5;    // Washer radius for 12V wires
    translate([0,z/2,0])
    cylinder(thickness+1,wr,wr,true);
}
// Material to be added to edge so cover fits. Outside edge.
module rim(x) {
    z = thickness/2;
    linear_extrude(height=x,center=true,convexity=10) {
        square([z,z],center=true); 
    }
}
  
// -------- Final Assembly --------
module final_assembly(x,y,z) {
    // Bottom
    color("gray")
    translate([0,0,thickness/2])
    base(x,y);
    
    // DC End
    rotate([90,0,90])
    translate([0,(z+thickness)/2,-(x+thickness)/2])
    difference() {
        union() {
            color("red")
            rotate([0,90,0])
            translate([thickness/4,z/2+thickness/2,0])
            rim(y+2*thickness);
            color("lightgreen")
            end(y+2*thickness,z+thickness/2);
        }
        dc_wire_opening(z);
    }
    
    // AC End
    rotate([90,0,90])
    translate([0,(z+thickness)/2,(x+thickness)/2])
    union() {
        color("green")
        end(y+2*thickness,z+thickness/2);
        rotate([0,90,0])
        translate([-thickness/4,z/2+thickness/4,0])
        color("red")
        rim(y+2*thickness);
    }
    
    // Side
    rotate([90,0,0])
    translate([0,(z+thickness)/2,(y+thickness)/2])
    union() {
        color("green")
        side(x,z+thickness/2);
        color("red")
        rotate([0,90,0])
        translate([-thickness/4,z/2+thickness/4,0])
        rim(x+thickness);
    }
    
    // Side
    rotate([90,0,0])
    translate([0,(z+thickness)/2,-(y+thickness)/2])
    union() {
        color("green")
        side(x,z+thickness/2);
        color("red")
        rotate([0,90,0])
        translate([thickness/4,z/2+thickness/4,0])
        rim(x+thickness);
    }
    
}

final_assembly(length,width,height);