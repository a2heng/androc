/**
 * The stable, compile-time contract shared by the Androc series.
 *
 * <p>Rules:</p>
 * <ul>
 *   <li>No Android framework types, no third-party libraries.</li>
 *   <li>No behaviour, no process or file access: implementations live in the
 *       kernel, tools and UI modules.</li>
 *   <li>Additive changes only within a major version. Every product pins a
 *   contracts version.</li>
 * </ul>
 */
package com.a2heng.androc.contracts;
