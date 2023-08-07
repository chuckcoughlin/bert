// OpenSCAD for the bottom of a box to hold the power supplies
//          110V 60hz to 12V conversion for Bert
//          Origin = center, bottom of bottom plate

height    = 37;        // Inside height (1/2 high)
length    =240;        // Inside length
rib_thickness = 3;     // Thickness of ribs and other supports
thickness =  3;        // Wall and bottom thickness
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
    // Dimension C-13 female plug socket
    plug_height = 22;  // Socket height
    plug_width  = 30;  // Socket width
    plug_top_edge=16;
    
    // Rectangle
    linear_extrude(thickness+1,center=true,convexity=10) {
       polygon([[-plug_width/2,plug_height/2],
                [plug_width/2,plug_height/2],
                [plug_width/2,-plug_height/2],
                [-plug_width/2,-plug_height/2]]); 
    } 
    translate([screw_separation/2,0,0])
    cylinder(thickness+1,screw_radius,screw_radius,true);
    translate([-screw_separation/2,0,0])
    cylinder(thickness+1,screw_radius,screw_radius,true);
}
// Material to be added to top edge so cover fits. Outside edge.
// Arguments are the length and with of the band.
module band(x,w) {
    z = rib_thickness;
    linear_extrude(height=x,center=true,convexity=10) {
        square([z,w],center=true); 
    }
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
module dc_grommet_opening(z) {
    wr  = 12;    // Grommet radius for 12V wires
    translate([0,z/2,0])
    cylinder(thickness+1,wr,wr,true);
}
// Half circle opening
module dc_wire_opening(z) {
    wr  = 5;    // Washer radius for 12V wires
    translate([0,z/2,0])
    cylinder(thickness+1,wr,wr,true);
}
// The ribs help stiffen the sides and 
// hold the transformer in place. The argument is
// the length of the rib
module rib(h) {
    z = rib_thickness;
    linear_extrude(height=h,center=false,convexity=10) {
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
            translate([thickness,z-15,0])
            band(y+2*thickness,10);
            color("lightgreen")
            end(y+2*thickness,z+thickness/2);
        }
        union() {
            dc_wire_opening(z);
            translate([0,0,-thickness])  // Only the "band"
            dc_grommet_opening(z);
        }
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
        translate([-thickness,z-15,0])
        band(y+2*thickness,10);
    }
    
    // Side
    rotate([90,0,0])
    translate([0,(z+thickness)/2,(y+thickness)/2])
    union() {
        color("green")
        side(x,z+thickness/2);
        rotate([90,0,0])
        translate([0,thickness/2,thickness/2])
        band(y/2,5);
        rotate([0,90,0])
        translate([-thickness,z/2+5-thickness/2,0])
        band(x+4*thickness,10);
    }
    
    // Side
    rotate([90,0,0])
    translate([0,(z+thickness)/2,-(y+thickness)/2])
    union() {
        color("green")
        side(x,z+thickness/2);
        rotate([90,0,0])
        translate([0,-thickness/2,thickness/2])
        band(y/2,5);
        rotate([0,90,0])
        translate([thickness,z/2+5-thickness/2,0])
        band(x+4*thickness,10);
    }
    // Interior support
    union() { 
        color("black");
        offset = 30-length/2;
        translate([offset,width/2,thickness/2])
        rib(height);
        translate([offset,-width/2,thickness/2])
        rib(height);
        rotate([90,0,0])
        translate([offset,thickness,-width/2])
        rib(width);
        
    }
    union() { 
        color("red");
        offset = length/2-75;
        translate([offset,width/2,thickness/2])
        rib(height);
        translate([offset,-width/2,thickness/2])
        rib(height);
        rotate([90,0,0])
        translate([offset,thickness,-width/2])
        rib(width);  
    }
}

final_assembly(length,width,height);