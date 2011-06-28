package net.piemaster.jario.systems;

import net.piemaster.jario.components.Acceleration;
import net.piemaster.jario.components.CollisionMesh;
import net.piemaster.jario.components.Health;
import net.piemaster.jario.components.Physical;
import net.piemaster.jario.components.Transform;
import net.piemaster.jario.components.Velocity;

import org.newdawn.slick.geom.Polygon;

import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.EntitySystem;
import com.artemis.utils.ImmutableBag;

public class CollisionSystem extends EntitySystem
{
	private ComponentMapper<Transform> transformMapper;
	private ComponentMapper<Velocity> velocityMapper;
	private ComponentMapper<Physical> physicalMapper;
	private ComponentMapper<Health> healthMapper;
	private ComponentMapper<CollisionMesh> meshMapper;

	// public static final int EDGE_NONE = 0;
	// public static final int EDGE_TOP = 1;
	// public static final int EDGE_BOTTOM = 2;
	// public static final int EDGE_LEFT = 4;
	// public static final int EDGE_RIGHT = 8;

	public static enum EdgeType
	{
		EDGE_NONE, EDGE_TOP, EDGE_BOTTOM, EDGE_LEFT, EDGE_RIGHT
	};
	
	@SuppressWarnings("unchecked")
	public CollisionSystem()
	{
		super(Transform.class, CollisionMesh.class);
	}

	@Override
	public void initialize()
	{
		transformMapper = new ComponentMapper<Transform>(Transform.class, world.getEntityManager());
		velocityMapper = new ComponentMapper<Velocity>(Velocity.class, world.getEntityManager());
		physicalMapper = new ComponentMapper<Physical>(Physical.class, world.getEntityManager());
		healthMapper = new ComponentMapper<Health>(Health.class, world.getEntityManager());
		meshMapper = new ComponentMapper<CollisionMesh>(CollisionMesh.class,
				world.getEntityManager());
	}

	@Override
	protected void processEntities(ImmutableBag<Entity> entities)
	{
		Entity player = world.getTagManager().getEntity("PLAYER");
		ImmutableBag<Entity> terrain = world.getGroupManager().getEntities("TERRAIN");

		if (terrain != null && (terrain != null || player != null))
		{
			for (int a = 0; terrain.size() > a; a++)
			{
				Entity rock = terrain.get(a);

				// if (bullets != null)
				// {
				// for (int b = 0; bullets.size() > b; b++)
				// {
				// Entity bullet = bullets.get(b);
				//
				// if (collisionExists(bullet, rock))
				// {
				// Transform tb = transformMapper.get(bullet);
				// EntityFactory.createBulletExplosion(world, tb.getX(),
				// tb.getY())
				// .refresh();
				//
				// world.deleteEntity(bullet);
				// world.getGroupManager().remove(bullet);
				// bullets = world.getGroupManager().getEntities("BULLETS");
				//
				// Health health = healthMapper.get(rock);
				// health.addDamage(1);
				//
				// if (!health.isAlive())
				// {
				// handleAsteroidCollision(rock);
				// player.getComponent(Score.class).incrementScore();
				//
				// continue rockLoop;
				// }
				// }
				// }
				// }

				if (player != null)
				{
					if (healthMapper.get(player).isAlive()
							&& player.getComponent(Physical.class).isMoving()
							&& collisionExists(player, rock))
					{
						
						EdgeType edge = detectCollisionEdge(player, rock);

						System.out.println("Player colliding with block, on edge "+edge);
						
						placeEntityOnTerrain(player, rock, reverseEdge(edge));
					}
				}
			}
		}
	}


	private boolean collisionExists(Entity e1, Entity e2)
	{
		Polygon p1 = meshMapper.get(e1).getPoly();
		Polygon p2 = meshMapper.get(e2).getPoly();
		return p1.intersects(p2);
	}

	/**
	 * Detects the edge of the collision between the collision meshes of two
	 * entities.
	 * 
	 * @param e1
	 *            The entity of interest.
	 * @param e2
	 *            The entity being collided with.
	 * @return The edge of entity 1 that collided with entity 2.
	 */
	private EdgeType detectCollisionEdge(Entity e1, Entity e2)
	{
		Polygon p1 = meshMapper.get(e1).getPoly();
		Polygon p2 = meshMapper.get(e2).getPoly();

		// Get the X collision value
		float xColl;
		if (p1.getX() < p2.getX())
			xColl = (p1.getX() + p1.getWidth()) - p2.getX();
		else
			xColl = -((p2.getX() + p2.getWidth()) - p1.getX());

		// Get the Y collision value
		float yColl;
		if (p1.getY() < p2.getY())
			yColl = (p1.getY() + p1.getHeight()) - p2.getY();
		else
			yColl = -((p2.getY() + p2.getHeight()) - p1.getY());

		// The collision is from the direction with the smaller value
		// If the collision value is positive, it's top/left
		EdgeType collEdge;
		if (Math.abs(xColl) < Math.abs(yColl))
		{
			collEdge = (xColl > 0) ? EdgeType.EDGE_RIGHT : EdgeType.EDGE_LEFT;
		}
		else
		{
			collEdge = (yColl > 0) ? EdgeType.EDGE_BOTTOM : EdgeType.EDGE_TOP;
		}
		return collEdge;
	}
	

	/**
	 * Set position and stop relevant movement.
	 */
	void placeEntityOnTerrain(Entity e1, Entity e2, EdgeType edge)
	{
		Polygon p1 = meshMapper.get(e1).getPoly();
		Polygon p2 = meshMapper.get(e2).getPoly();
		Transform t1 = transformMapper.get(e1);
		Velocity v1 = velocityMapper.get(e1);
		Physical phys = physicalMapper.get(e1);
		
		if(edge == EdgeType.EDGE_TOP)
		{
			// Set the Y coordinate to that of the terrain object
			t1.setY(p2.getY() - p1.getHeight());
			// Zero any vertical movement if moving towards terrain
			if(v1.getY() >= 0)
			{
				haltVertical(e1);
			}
			// Record that the actor is on the ground to avoid gravity
			phys.setGrounded(true);
		}
		else if(edge == EdgeType.EDGE_BOTTOM)
		{
			t1.setY(p2.getY() + p2.getHeight());
			if(v1.getY() <= 0)
			{
				haltVertical(e1);
			}
			phys.setGrounded(false);
		}
		else if(edge == EdgeType.EDGE_LEFT)
		{
			t1.setX(p2.getX() - p1.getWidth());
			haltHorizontal(e1);
		}
		else if(edge == EdgeType.EDGE_RIGHT)
		{
			t1.setX(p2.getX() + p2.getWidth());
			haltHorizontal(e1);
		}
		// Update the collision mesh
		p1.setLocation(t1.getX(), t1.getY());
	}
	
	private void haltVertical(Entity ent)
	{
		ent.getComponent(Acceleration.class).setY(0);
		ent.getComponent(Velocity.class).setY(0);
	}

	private void haltHorizontal(Entity ent)
	{
		ent.getComponent(Acceleration.class).setX(0);
		ent.getComponent(Velocity.class).setX(0);
	}

	private EdgeType reverseEdge(EdgeType edge)
	{
		switch(edge)
		{
		case EDGE_LEFT:
			return EdgeType.EDGE_RIGHT;
		case EDGE_RIGHT:
			return EdgeType.EDGE_LEFT;
		case EDGE_TOP:
			return EdgeType.EDGE_BOTTOM;
		case EDGE_BOTTOM:
			return EdgeType.EDGE_TOP;
		default:
			return EdgeType.EDGE_NONE;
		}
	}
	
	@Override
	protected boolean checkProcessing()
	{
		return true;
	}

}
