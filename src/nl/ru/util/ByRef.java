/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.ru.util;

/**
 *
 * @author Erwin
 */
public final class ByRef<T> {
  public T argValue;
  public ByRef(T refArg)
  {
          argValue = refArg;
  }
}
