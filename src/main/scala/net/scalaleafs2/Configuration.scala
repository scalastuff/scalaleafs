/**
 * Copyright (c) 2012 Ruud Diterwich.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package net.scalaleafs2

class Configuration (assignments : ConfigVar.Assignment[_]*) {
  
  private[scalaleafs2] val values : Map[ConfigVar[_], Any] = assignments.toMap
  
  def withDefaults(defaults : ConfigVar.Assignment[_]*) = {
    new Configuration(assignments ++ defaults.filterNot(a => values.contains(a._1)):_*)
  }
  
  def &(assignments : ConfigVar.Assignment[_]*) = {
    new Configuration(this.assignments ++ assignments:_*)
  }
}

abstract class ConfigVar[A](val defaultValue : A) {
  
  def apply(context : Context) : A =
    apply(context.site.configuration)
    
  def apply(configuration : Configuration) : A = 
    configuration.values.get(this).map(_.asInstanceOf[A]).getOrElse(defaultValue)
    
  def get(implicit configuration : Configuration) : A = 
    configuration.values.get(this).map(_.asInstanceOf[A]).getOrElse(defaultValue)
}

object ConfigVar {
  
  // Type def to make sure each individual tuple has matching types for var and value.
  type Assignment[A] = Tuple2[ConfigVar[A], A]
  
  // Implicitly convert a configVar value to its value. 
  implicit def getFromConfig[A](configVar : ConfigVar[A])(implicit configuration : Configuration) = configVar.apply (configuration)
  
  // Implicitly convert a configVar value to its value. 
//?  implicit def getfromContext[A](configVar : ConfigVar[A])(implicit context : Context) = configVar.apply (context)

}
