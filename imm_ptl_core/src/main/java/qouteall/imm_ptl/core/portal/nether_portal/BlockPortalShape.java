package qouteall.imm_ptl.core.portal.nether_portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.portal.GeometryPortalShape;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.IntBox;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BlockPortalShape {
    public BlockPos anchor;
    public Set<BlockPos> area;
    public IntBox innerAreaBox;
    public IntBox totalAreaBox;
    public Direction.Axis axis;
    public Set<BlockPos> frameAreaWithoutCorner;
    public Set<BlockPos> frameAreaWithCorner;
    
    public BlockPos firstFramePos;
    
    public BlockPortalShape(
        Set<BlockPos> area, Direction.Axis axis
    ) {
        this.area = area;
        this.axis = axis;
        
        calcAnchor();
        
        calcFrameArea();
        
        calcAreaBox();
    }
    
    public BlockPortalShape(
        CompoundTag tag
    ) {
        this(
            readArea(tag.getList("poses", 3)),
            Direction.Axis.values()[tag.getInt("axis")]
        );
    }
    
    private static Set<BlockPos> readArea(ListTag list) {
        int size = list.size();
        
        Validate.isTrue(size % 3 == 0);
        Set<BlockPos> result = new HashSet<>();
        
        for (int i = 0; i < size / 3; i++) {
            result.add(new BlockPos(
                list.getInt(i * 3 + 0),
                list.getInt(i * 3 + 1),
                list.getInt(i * 3 + 2)
            ));
        }
        
        return result;
    }
    
    
    public CompoundTag toTag() {
        CompoundTag data = new CompoundTag();
        ListTag list = new ListTag();
        
        area.forEach(blockPos -> {
            list.add(list.size(), IntTag.valueOf(blockPos.getX()));
            list.add(list.size(), IntTag.valueOf(blockPos.getY()));
            list.add(list.size(), IntTag.valueOf(blockPos.getZ()));
        });
        
        data.put("poses", list);
        data.putInt("axis", axis.ordinal());
        
        return data;
    }
    
    public void calcAnchor() {
        anchor = area.stream()
            .min(
                Comparator.<BlockPos>comparingInt(
                    Vec3i::getX
                ).<BlockPos>thenComparingInt(
                    Vec3i::getY
                ).<BlockPos>thenComparingInt(
                    Vec3i::getZ
                )
            ).get();
        
        Validate.notNull(anchor);
    }
    
    public void calcAreaBox() {
        innerAreaBox = Helper.reduce(
            new IntBox(anchor, anchor),
            area.stream(),
            IntBox::getExpanded
        );
        totalAreaBox = Helper.reduce(
            new IntBox(anchor, anchor),
            frameAreaWithoutCorner.stream(),
            IntBox::getExpanded
        );
    }
    
    public void calcFrameArea() {
        Direction[] directions = Helper.getAnotherFourDirections(axis);
        frameAreaWithoutCorner = area.stream().flatMap(
            blockPos -> Stream.of(
                blockPos.offset(directions[0].getNormal()),
                blockPos.offset(directions[1].getNormal()),
                blockPos.offset(directions[2].getNormal()),
                blockPos.offset(directions[3].getNormal())
            )
        ).filter(
            blockPos -> !area.contains(blockPos)
        ).collect(Collectors.toSet());
        
        BlockPos[] cornerOffsets = {
            new BlockPos(directions[0].getNormal()).offset(directions[1].getNormal()),
            new BlockPos(directions[1].getNormal()).offset(directions[2].getNormal()),
            new BlockPos(directions[2].getNormal()).offset(directions[3].getNormal()),
            new BlockPos(directions[3].getNormal()).offset(directions[0].getNormal())
        };
        
        frameAreaWithCorner = area.stream().flatMap(
            blockPos -> Stream.of(
                blockPos.offset(cornerOffsets[0]),
                blockPos.offset(cornerOffsets[1]),
                blockPos.offset(cornerOffsets[2]),
                blockPos.offset(cornerOffsets[3])
            )
        ).filter(
            blockPos -> !area.contains(blockPos)
        ).collect(Collectors.toSet());
        frameAreaWithCorner.addAll(frameAreaWithoutCorner);
        
        firstFramePos = frameAreaWithoutCorner.iterator().next();
    }
    
    //null for not found
    public static BlockPortalShape findArea(
        BlockPos startingPos,
        Direction.Axis axis,
        Predicate<BlockPos> isAir,
        Predicate<BlockPos> isObsidian
    ) {
        if (!isAir.test(startingPos)) {
            return null;
        }
        
        return findShapeWithoutRegardingStartingPos(startingPos, axis, isAir, isObsidian);
    }
    
    public static BlockPortalShape findShapeWithoutRegardingStartingPos(
        BlockPos startingPos, Direction.Axis axis, Predicate<BlockPos> isAir, Predicate<BlockPos> isObsidian
    ) {
        startingPos = startingPos.immutable();
        
        Set<BlockPos> area = new HashSet<>();
        area.add(startingPos);
        
        Direction[] directions = Helper.getAnotherFourDirections(axis);
        boolean isNormalFrame = findAreaBreadthFirst(
            startingPos,
            isAir,
            isObsidian,
            directions,
            area,
            startingPos
        );
        
        if (!isNormalFrame) {
            return null;
        }
        
        return new BlockPortalShape(area, axis);
    }
    
    private static boolean findAreaBreadthFirst(
        BlockPos startingPos,
        Predicate<BlockPos> isAir,
        Predicate<BlockPos> isObsidian,
        Direction[] directions,
        Set<BlockPos> foundArea,
        BlockPos initialPos
    ) {
        
        ArrayDeque<BlockPos> newlyAdded = new ArrayDeque<>();
        newlyAdded.addLast(startingPos);
        
        while (!newlyAdded.isEmpty()) {
            if (foundArea.size() > 400) {
                return false;
            }
            
            BlockPos last = newlyAdded.pollFirst();
            for (Direction direction : directions) {
                BlockPos curr = last.relative(direction).immutable();
                if (!foundArea.contains(curr)) {
                    if (isAir.test(curr)) {
                        newlyAdded.addLast(curr);
                        foundArea.add(curr);
                    }
                    else if (isObsidian.test(curr)) {
                        //nothing happens
                    }
                    else {
                        return false;
                    }
                }
            }
            
            BlockPos delta = initialPos.subtract(startingPos);
            if (Math.abs(delta.getX()) > 20 || Math.abs(delta.getY()) > 20 || Math.abs(delta.getZ()) > 20) {
                return false;
            }
        }
        
        return true;
    }
    
    @Deprecated
    private static boolean findAreaRecursively(
        BlockPos currPos,
        Predicate<BlockPos> isAir,
        Predicate<BlockPos> isObsidian,
        Direction[] directions,
        Set<BlockPos> foundArea,
        BlockPos initialPos
    ) {
        if (foundArea.size() > 400) {
            return false;
        }
        BlockPos delta = initialPos.subtract(currPos);
        if (Math.abs(delta.getX()) > 20 || Math.abs(delta.getY()) > 20 || Math.abs(delta.getZ()) > 20) {
            return false;
        }
        for (Direction direction : directions) {
            BlockPos newPos = currPos.offset(direction.getNormal());
            if (!foundArea.contains(newPos)) {
                if (isAir.test(newPos)) {
                    foundArea.add(newPos.immutable());
                    boolean shouldContinue = findAreaRecursively(
                        newPos,
                        isAir,
                        isObsidian,
                        directions,
                        foundArea,
                        initialPos
                    );
                    if (!shouldContinue) {
                        return false;
                    }
                }
                else {
                    if (!isObsidian.test(newPos)) {
                        //abort
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    //return null for not match
    public BlockPortalShape matchShape(
        Predicate<BlockPos> isAir,
        Predicate<BlockPos> isObsidian,
        BlockPos newAnchor,
        BlockPos.MutableBlockPos temp
    ) {
        if (!isAir.test(newAnchor)) {
            return null;
        }
        
        boolean testFrame = testFrameWithoutCorner(isObsidian, newAnchor, temp);
        
        if (!testFrame) {
            return null;
        }
        
        boolean testAir = area.stream().map(
            blockPos -> temp.set(
                blockPos.getX() - anchor.getX() + newAnchor.getX(),
                blockPos.getY() - anchor.getY() + newAnchor.getY(),
                blockPos.getZ() - anchor.getZ() + newAnchor.getZ()
            )
            //blockPos.subtract(anchor).add(newAnchor)
        ).allMatch(
            isAir
        );
        
        if (!testAir) {
            return null;
        }
        
        return getShapeWithMovedAnchor(newAnchor);
    }
    
    private boolean testFrameWithoutCorner(
        Predicate<BlockPos> isObsidian,
        BlockPos newAnchor,
        BlockPos.MutableBlockPos temp
    ) {
        Function<BlockPos, BlockPos.MutableBlockPos> mapper = blockPos -> temp.set(
            blockPos.getX() - anchor.getX() + newAnchor.getX(),
            blockPos.getY() - anchor.getY() + newAnchor.getY(),
            blockPos.getZ() - anchor.getZ() + newAnchor.getZ()
        );
        
        //does this have optimization effect?
        if (!isObsidian.test(mapper.apply(firstFramePos))) {
            return false;
        }
        
        return frameAreaWithoutCorner.stream().map(mapper).allMatch(isObsidian);
    }
    
    public BlockPortalShape getShapeWithMovedAnchor(
        BlockPos newAnchor
    ) {
        BlockPos offset = newAnchor.subtract(anchor);
        return new BlockPortalShape(
            area.stream().map(
                blockPos -> blockPos.offset(offset)
            ).collect(Collectors.toSet()),
            axis
        );
    }
    
    public boolean isFrameIntact(
        Predicate<BlockPos> isObsidian
    ) {
        return frameAreaWithoutCorner.stream().allMatch(isObsidian::test);
    }
    
    public boolean isPortalIntact(
        Predicate<BlockPos> isPortalBlock,
        Predicate<BlockPos> isObsidian
    ) {
        return isFrameIntact(isObsidian) &&
            area.stream().allMatch(isPortalBlock);
    }
    
    public void initPortalPosAxisShape(Portal portal, boolean doInvert) {
        Vec3 center = innerAreaBox.getCenterVec();
        portal.setPos(center.x, center.y, center.z);
        
        Direction[] anotherFourDirections = Helper.getAnotherFourDirections(axis);
        Direction wDirection;
        Direction hDirection;
        if (doInvert) {
            wDirection = anotherFourDirections[0];
            hDirection = anotherFourDirections[1];
        }
        else {
            wDirection = anotherFourDirections[1];
            hDirection = anotherFourDirections[0];
        }
        portal.axisW = Vec3.atLowerCornerOf(wDirection.getNormal());
        portal.axisH = Vec3.atLowerCornerOf(hDirection.getNormal());
        portal.width = Helper.getCoordinate(innerAreaBox.getSize(), wDirection.getAxis());
        portal.height = Helper.getCoordinate(innerAreaBox.getSize(), hDirection.getAxis());
        
        Vec3 offset = Vec3.atLowerCornerOf(
            Direction.get(Direction.AxisDirection.POSITIVE, axis)
                .getNormal()
        ).scale(0.5);
        
        if (isRectangle()) {
            portal.specialShape = null;
        }
        else {
            GeometryPortalShape shape = new GeometryPortalShape();
            
            IntBox rectanglePart = Helper.expandRectangle(
                anchor,
                blockPos -> area.contains(blockPos),
                axis
            );
            
            Stream.concat(
                area.stream()
                    .filter(blockPos -> !rectanglePart.contains(blockPos))
                    .map(blockPos -> new IntBox(blockPos, blockPos)),
                Stream.of(rectanglePart)
            ).forEach(part -> {
                Vec3 p1 = Vec3.atLowerCornerOf(part.l).add(offset);
                Vec3 p2 = Vec3.atLowerCornerOf(part.h).add(1, 1, 1).add(offset);
                double p1LocalX = p1.subtract(center).dot(portal.axisW);
                double p1LocalY = p1.subtract(center).dot(portal.axisH);
                double p2LocalX = p2.subtract(center).dot(portal.axisW);
                double p2LocalY = p2.subtract(center).dot(portal.axisH);
                shape.addTriangleForRectangle(
                    p1LocalX, p1LocalY,
                    p2LocalX, p2LocalY
                );
            });
            
            portal.specialShape = shape;
            
            Vec3 p1 = Vec3.atLowerCornerOf(rectanglePart.l).add(offset);
            Vec3 p2 = Vec3.atLowerCornerOf(rectanglePart.h).add(1, 1, 1).add(offset);
            double p1LocalX = p1.subtract(center).dot(portal.axisW);
            double p1LocalY = p1.subtract(center).dot(portal.axisH);
            double p2LocalX = p2.subtract(center).dot(portal.axisW);
            double p2LocalY = p2.subtract(center).dot(portal.axisH);
            portal.initCullableRange(
                p1LocalX, p2LocalX,
                p1LocalY, p2LocalY
            );
        }
    }
    
    public BlockPortalShape matchShapeWithMovedFirstFramePos(
        Predicate<BlockPos> isAir,
        Predicate<BlockPos> isObsidian,
        BlockPos newFirstObsidianPos,
        BlockPos.MutableBlockPos temp
    ) {
        boolean testFrame = frameAreaWithoutCorner.stream().map(blockPos1 -> temp.set(
            blockPos1.getX() - firstFramePos.getX() + newFirstObsidianPos.getX(),
            blockPos1.getY() - firstFramePos.getY() + newFirstObsidianPos.getY(),
            blockPos1.getZ() - firstFramePos.getZ() + newFirstObsidianPos.getZ()
        )).allMatch(isObsidian);
        
        if (!testFrame) {
            return null;
        }
        
        boolean testAir = area.stream().map(blockPos -> temp.set(
            blockPos.getX() - firstFramePos.getX() + newFirstObsidianPos.getX(),
            blockPos.getY() - firstFramePos.getY() + newFirstObsidianPos.getY(),
            blockPos.getZ() - firstFramePos.getZ() + newFirstObsidianPos.getZ()
        )).allMatch(isAir);
        
        if (!testAir) {
            return null;
        }
        
        BlockPos offset = newFirstObsidianPos.subtract(firstFramePos);
        return new BlockPortalShape(
            area.stream().map(
                blockPos -> blockPos.offset(offset)
            ).collect(Collectors.toSet()),
            axis
        );
    }
    
    public static boolean isSquareShape(BlockPortalShape shape, int length) {
        BlockPos areaSize = shape.innerAreaBox.getSize();
        
        Tuple<Direction.Axis, Direction.Axis> xs = Helper.getAnotherTwoAxis(shape.axis);
        
        return Helper.getCoordinate(areaSize, xs.getA()) == length &&
            Helper.getCoordinate(areaSize, xs.getB()) == length &&
            shape.area.size() == (length * length);
    }
    
    public static BlockPortalShape getSquareShapeTemplate(
        Direction.Axis axis,
        int length
    ) {
        Tuple<Direction, Direction> perpendicularDirections = Helper.getPerpendicularDirections(
            Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE)
        );
        
        Set<BlockPos> area = new HashSet<>();
        
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < length; j++) {
                area.add(
                    BlockPos.ZERO.relative(perpendicularDirections.getA(), i)
                        .relative(perpendicularDirections.getB(), j)
                );
            }
        }
        
        return new BlockPortalShape(area, axis);
    }
    
    public BlockPortalShape getShapeWithMovedTotalAreaBox(IntBox newTotalAreaBox) {
        Validate.isTrue(totalAreaBox.getSize().equals(newTotalAreaBox.getSize()));
        
        return getShapeWithMovedAnchor(
            newTotalAreaBox.l.subtract(totalAreaBox.l)
                .offset(anchor)
        );
    }
    
    public boolean isRectangle() {
        BlockPos size = innerAreaBox.getSize();
        return size.getX() * size.getY() * size.getZ() == area.size();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockPortalShape that = (BlockPortalShape) o;
        return area.equals(that.area) &&
            axis == that.axis;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(area, axis);
    }
}
