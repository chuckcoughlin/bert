// OpenSCAD for the bottom of a box to hold the power supplies
//          110V 60hz to 12V conversion for Bert
//          Origin = center, bottom of bottom plate

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

module ac_wire_opening() {
    screw_separation = 40; // center-center between screws
    screw_radius = 2.5;    // screw hole in socket
    // Dimension C-13 female plug
    plug_height = 20;  // Socket height
    plug_width  = 26;  // Socket width
    plug_top_edge=16;
    plug_cut = (plug_width-plug_top_edge)/2; // diagonal
    
    
    linear_extrude(thickness+1,center=true,convexity=10) {
       polygon([[-plug_width/2,plug_height/2],
                [plug_width/2,plug_height/2],
                [plug_width/2,plug_cut-plug_height/2],
                [plug_width/2-plug_cut,-plug_height/2],
                [-plug_width/2+plug_cut,-plug_height/2],
                [-plug_width/2,plug_cut-plug_height/2]]); 
    } 
    translate([screw_separation/2,0,0])
    cylinder(thickness+1,screw_radius,screw_radius,true);
    translate([-screw_separation/2,0,0])
    cylinder(thickness+1,screw_radius,screw_radius,true);
}

module bottom_screw_holes(x) {
    dx = 15;           // Screw hole offset from end
    sr = 2.5;          // Radius for screw hole
    translate([x/2-dx,0,0])
    cylinder(thickness+1,sr,sr,true,$fn=6);
    translate([dx-x/2,0,0])
    cylinder(thickness+1,sr,sr,true,$fn=6);
}
// Half circle opening
module dc_wire_opening(z) {
    wr  = 5;    // Washer radius for 12V wires
    translate([0,z/2,0])
    cylinder(thickness+1,wr,wr,true);
}
// Edge on upper inside to for slide-on fit. Inside edge
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
    difference() {
        base(x,y);
        bottom_screw_holes(x);
    }
    
    // DC End
    rotate([90,0,90])
    translate([0,(z+thickness)/2,-(x+thickness)/2])
    difference() {
        union() {
            color("red")
            rotate([0,90,0])
            translate([-thickness/4,z/2+thickness/4,0])
            rim(y);
            color("lightgreen")
            end(y+2*thickness,z+thickness/2);
        }
        dc_wire_opening(z);
    }
    
    // AC End
    rotate([90,0,90])
    translate([0,(z+thickness)/2,(x+thickness)/2])
    union() {
        difference() {
            color("green")
            end(y+2*thickness,z+thickness/2);
            ac_wire_opening();
        }
        color("red")
        rotate([0,90,0])
        translate([thickness/4,z/2+thickness/4,0])
        rim(y);
    }
    
    // Side
    rotate([90,0,0])
    translate([0,(z+thickness)/2,(y+thickness)/2])
    union() {
        color("green")
        side(x,z+thickness/2);
        rotate([0,90,0])
        translate([thickness/4,z/2+thickness/4,0])
        color("red")
        rim(x+thickness);
    }
    
    // Side
    rotate([90,0,0])
    translate([0,(z+thickness)/2,-(y+thickness)/2])
    union() {
        color("green")
        side(x,z+thickness/2);
        rotate([0,90,0])
        translate([-thickness/4,z/2+thickness/4,0])
        color("red")
        rim(x+thickness);
    }
    
}

final_assembly(length,width,height);