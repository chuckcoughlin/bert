// OpenSCAD for the top of a box to hold the power supplies
//          110V 60hz to 12V conversion for Bert
//          Origin = center, bottom of top plate
//          Orientation is upside down

height    = 35;        // Inside height (1/2 high)
length    =240;        // Inside length
thickness =  4;        // Wall and bottom thickness
width     = 70;        // Inside width

// Bottom plate. 
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

// Material to be subtracted from upper rim so cover fits
module rim_indentation(x) {
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
    color("lightgreen")
    rotate([90,0,90])
    translate([0,(z+thickness)/2,-(x+thickness)/2])
    difference() {
        end(y+2*thickness,z+thickness);
        rotate([0,90,0])
        translate([thickness/4,z/2+thickness/4,0])
        rim_indentation(y);
    }
    
    // AC End
    color("green")
    rotate([90,0,90])
    translate([0,(z+thickness)/2,(x+thickness)/2])
    difference() {
        end(y+2*thickness,z+thickness);
        rotate([0,90,0])
        translate([-thickness/4,z/2+thickness/4,0])
        rim_indentation(y);
    }
    
    // Side
    color("green")
    rotate([90,0,0])
    translate([0,(z+thickness)/2,(y+thickness)/2])
    difference() {
        side(x,z+thickness);
        rotate([0,90,0])
        translate([-thickness/4,z/2+thickness/4,0])
        rim_indentation(x+thickness);
    }
    
    // Side
    color("green")
    rotate([90,0,0])
    translate([0,(z+thickness)/2,-(y+thickness)/2])
        difference() {
        side(x,z+thickness);
        rotate([0,90,0])
        translate([thickness/4,z/2+thickness/4,0])
        rim_indentation(x+thickness);
    }
    
}

final_assembly(length,width,height);