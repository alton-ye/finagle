package com.twitter.finagle.loadbalancer

import com.twitter.finagle.ServiceFactory
import com.twitter.util.{Time, Activity, Future}

/**
 * A Balancer mix-in which provides the collection over which to load balance
 * by observing `activity`.
 */
private[loadbalancer] trait Updating[Req, Rep] extends Balancer[Req, Rep] {
  /**
   * An activity representing the active set of ServiceFactories.
   */
  // Note: this is not a terribly good method name and should be
  // improved in a future commit.
  protected def activity: Activity[Traversable[ServiceFactory[Req, Rep]]]

  /*
   * Subscribe to the Activity and dynamically update the load
   * balancer as it (succesfully) changes.
   *
   * The observation is terminated when the Balancer is closed.
   */
  private[this] val observation = activity.states.respond {
    case Activity.Ok(newList) => update(newList)
    case _ => // nop
  }

  override def close(deadline: Time): Future[Unit] = {
    observation.close(deadline).before { super.close(deadline) }
  }
}
